/*
 *  Copyright 2025 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hu.bme.mit.theta.xcfa.passes

import hu.bme.mit.theta.core.stmt.*
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.abstracttype.Castable
import hu.bme.mit.theta.core.type.anytype.Dereference
import hu.bme.mit.theta.core.type.anytype.Exprs.Dereference
import hu.bme.mit.theta.core.type.booltype.BoolType
import hu.bme.mit.theta.core.type.bvtype.BvType
import hu.bme.mit.theta.core.type.inttype.IntType
import hu.bme.mit.theta.core.utils.TypeUtils.cast
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.frontend.transformation.model.types.complex.compound.CStruct
import hu.bme.mit.theta.xcfa.model.*
import hu.bme.mit.theta.xcfa.utils.READ
import hu.bme.mit.theta.xcfa.utils.WRITE
import hu.bme.mit.theta.xcfa.utils.dereferencesWithAccessType
import hu.bme.mit.theta.xcfa.utils.getFlatLabels
import kotlin.jvm.optionals.getOrNull

class UnionMemberAccessPass(val parseContext: ParseContext) : ProcedurePass {
    override fun run(builder: XcfaProcedureBuilder): XcfaProcedureBuilder {
        builder.getEdges().toMutableList().forEach { instrumentReads(builder, it) }
        builder.getEdges().toMutableList().forEach { instrumentWrites(builder, it) }
        return builder
    }

    private fun instrumentReads(builder: XcfaProcedureBuilder, edge: XcfaEdge) {
        val newEdges = edge.splitIfI { label ->
            label.dereferencesWithAccessType.any { (_, access) -> access == READ }
        }

        if (newEdges.none { (_, hasRead) -> hasRead }) return

        builder.removeEdge(edge)
        newEdges.map { (newEdge, hasRead) ->
            if (!hasRead) return@map newEdge

            val label = (edge.label as SequenceLabel).labels[0]
            val newLabel = label.run { when (this) {
                is StmtLabel -> StmtLabel(stmt.wrapDeref(builder))
                is InvokeLabel -> InvokeLabel(name, params.map { it.wrapDeref(builder) }, metadata, tempLookup)
                is StartLabel -> StartLabel(name, params.map { it.wrapDeref(builder) }, pidVar, metadata, tempLookup)
                else -> this
            } }

            newEdge.withLabel(SequenceLabel(listOf(newLabel)))
        }.forEach(builder::addEdge)
    }

    private fun instrumentWrites(builder: XcfaProcedureBuilder, edge: XcfaEdge) {
        val newEdges = edge.splitIfI { label ->
            label.dereferencesWithAccessType.any { (_, access) -> access == WRITE }
        }

        if (newEdges.none { (_, hasWrite) -> hasWrite }) return

        builder.removeEdge(edge)
        newEdges.map { (newEdge, hasWrite) ->
            if (!hasWrite) return@map newEdge

            val stmt = ((edge.label as SequenceLabel).labels[0] as StmtLabel).stmt

            if (stmt !is MemoryAssignStmt<*, *, *>) return@map newEdge

            val originalDeref = parseContext.metadata.getMetadataValue(stmt, "originalDeref")
                .getOrNull() as? Dereference<*, *, *> ?: return@map newEdge
            val compound = parseContext.metadata.getMetadataValue(originalDeref, "structType")
                .getOrNull() as? CStruct ?: return@map newEdge

            if (!compound.isUnion) return@map newEdge

            val deref = unionDeref(originalDeref, compound)
            val expr = punTo(stmt.expr, deref)

            newEdge.withLabel(SequenceLabel(listOf(StmtLabel(MemoryAssignStmt.create(deref, expr)))))
        }.forEach(builder::addEdge)
    }

    private fun Stmt.wrapDeref(builder: XcfaProcedureBuilder): Stmt {
        val stmt = when (this) {
            is AssignStmt<*> ->
                AssignStmt.of(
                    cast(varDecl, varDecl.type),
                    cast(expr.wrapDeref(builder), varDecl.type),
                )

            is AssumeStmt -> AssumeStmt.of(cond.wrapDeref(builder) as Expr<BoolType>)
            is SequenceStmt -> SequenceStmt.of(stmts.map { it.wrapDeref(builder) })
            is MemoryAssignStmt<*, *, *> -> MemoryAssignStmt.create(deref, expr.wrapDeref(builder))
            is IfStmt -> IfStmt.of(cond.wrapDeref(builder) as Expr<BoolType>, then.wrapDeref(builder), elze.wrapDeref(builder))
            is LoopStmt -> LoopStmt.of(stmt.wrapDeref(builder), loopVariable, from.wrapDeref(builder) as Expr<IntType>, to.wrapDeref(builder) as Expr<IntType>)
            is NonDetStmt -> NonDetStmt.of(stmts.map { it.wrapDeref(builder) })
            is HavocStmt<*> -> this
            is SkipStmt -> this
            else -> TODO("Not yet implemented")
        }
        val metadataValue = parseContext.metadata?.getMetadataValue(this, "sourceStatement")
        if (metadataValue?.isPresent == true)
            parseContext.metadata.create(stmt, "sourceStatement", metadataValue.get())
        return stmt
    }

    private fun Expr<*>.wrapDeref(builder: XcfaProcedureBuilder): Expr<*>
        = this.transform<Dereference<*, *, *>>(parseContext) { originalDeref ->
            val deref = Dereference(originalDeref.array.wrapDeref(builder), originalDeref.offset.wrapDeref(builder), originalDeref.type)
            val compound = parseContext.metadata.getMetadataValue(originalDeref, "structType")
                .getOrNull() as? CStruct ?: return@transform deref

            if (!compound.isUnion) return@transform deref

            val newDeref = unionDeref(originalDeref, compound)
            punTo(newDeref, originalDeref)
        }

    private fun unionDeref(deref: Dereference<*, *, *>, structType: CStruct) =
        Dereference(deref.array, structType.getValue("0"), BvType.of(32))

    private fun <C : Castable<C>> punTo(from: Expr<*>, to: Expr<*>): Expr<*> =
        (from as? Expr<C>)?.let { from.type?.Cast(it, to.type) } ?: from
}

private fun XcfaEdge.splitIfI(function: (XcfaLabel) -> Boolean): List<Pair<XcfaEdge, Boolean>> {
    val newLabels = mutableListOf<Pair<SequenceLabel, Boolean>>()
    var current = mutableListOf<XcfaLabel>()
    for (label in label.getFlatLabels()) {
        if (function(label)) {
            if (current.size > 0) {
                newLabels.add(Pair(SequenceLabel(current), false))
                current = mutableListOf()
            }
            newLabels.add(Pair(SequenceLabel(listOf(label)), true))
        } else {
            current.add(label)
        }
    }
    if (current.size > 0) newLabels.add(Pair(SequenceLabel(current), false))

    val locations = listOf(source) + newLabels.drop(1).map { (newLabel, _) -> // potentially metadata is off-by-one (i-2 might be suitable?)
        XcfaLocation("loc" + XcfaLocation.uniqueCounter(), metadata = newLabel.metadata)
    } + listOf(target)

    val newEdges = newLabels.mapIndexed { i, (newLabel, satisfiedFunction) ->
        Pair(XcfaEdge(locations[i], locations[i + 1], newLabel, metadata), satisfiedFunction)
    }
    return newEdges
}
