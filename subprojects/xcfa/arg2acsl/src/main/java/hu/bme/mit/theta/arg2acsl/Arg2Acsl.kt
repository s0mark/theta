/*
 *  Copyright 2024 Budapest University of Technology and Economics
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

package hu.bme.mit.theta.arg2acsl

import hu.bme.mit.theta.analysis.Action
import hu.bme.mit.theta.analysis.State
import hu.bme.mit.theta.analysis.algorithm.arg.ARG
import hu.bme.mit.theta.analysis.expl.ExplState
import hu.bme.mit.theta.analysis.expr.ExprState
import hu.bme.mit.theta.analysis.pred.PredState
import hu.bme.mit.theta.analysis.ptr.PtrState
import hu.bme.mit.theta.c2xcfa.CMetaData
import hu.bme.mit.theta.c2xcfa.getCMetaData
import hu.bme.mit.theta.core.decl.VarDecl
import hu.bme.mit.theta.core.type.anytype.RefExpr
import hu.bme.mit.theta.core.type.booltype.NotExpr
import hu.bme.mit.theta.core.type.inttype.IntEqExpr
import hu.bme.mit.theta.core.type.inttype.IntLitExpr
import hu.bme.mit.theta.core.type.inttype.IntType
import hu.bme.mit.theta.xcfa.analysis.XcfaState
import hu.bme.mit.theta.xcfa.analysis.reverseMapping
import hu.bme.mit.theta.xcfa.analysis.withGeneralizedVars
import hu.bme.mit.theta.xcfa.model.XcfaGlobalVar
import hu.bme.mit.theta.xcfa.model.XcfaLocation
import java.io.File
import java.math.BigInteger
import kotlin.streams.asSequence

fun PredState.toAcsl(): String? =
    if (this.preds.isEmpty()) null else
    this.preds.joinToString(prefix = "(", separator = ") && (", postfix = ")") { pred ->
        pred.toAcsl()
    }

fun ExplState.toAcsl(): String? =
    if (this.decls.isEmpty()) null else
    this.decls.joinToString(prefix = "(", separator = ") && (", postfix = ")") { decl ->
        "${decl.toAcsl()} == ${this.eval(decl).get().toAcsl()}"
    }

fun ExprState.toAcsl(): String? =
    when (this) {
        is ExplState -> this.toAcsl()
        is PredState -> this.toAcsl()
        is PtrState<*> -> this.innerState.toAcsl()
        else -> TODO("Conversion to ACSL is not implemented for the domain")
    }

fun<S : ExprState, A : Action> ARG<XcfaState<S>, A>.initialProcedureStates(): Map<XcfaLocation, Set<S>> =
    this.nodes.asSequence().filter { node -> // filter nodes in initial locations
        node.state.processes.values.firstOrNull()?.locs?.peek()?.initial ?: false
    }.groupByUnique( // group states by location
        { it.state.processes.values.first().locs.peek() },
        { node ->
            val varLookup = node.state.processes.values.firstOrNull()?.varLookup?.peek()?.reverseMapping() ?: mapOf()
            val globalVars = node.state.xcfa?.globalVars?.map(XcfaGlobalVar::wrappedVar) ?: listOf()
            val relevantVars = (varLookup.keys + varLookup.values + globalVars).filter { !it.name.endsWith("_ret") }
            node.state.withGeneralizedVars(relevantVars)
        }
    )

fun<S : ExprState, A : Action> ARG<XcfaState<S>, A>.writeAcslContracts(input: File, output: File) {
    // TODO add @requires \false to unreachable procedures?
    val lines = input.readLines().toMutableList()

    // remove global variable declarations
    val globalVarDecls = this.initNodes.asSequence().first().state.xcfa?.globalVars?.associate { globalVar ->
        globalVar.getCMetaData()!!.lineNumberStart!!.let {
            Pair(it, lines.removeAt(it - 1).apply { lines.add(it - 1, "") })
        }
    }?.apply { this.keys.sortedDescending().forEach { lines.removeAt(it - 1) } }

    // add contracts
    this.initialProcedureStates().toSortedMap { l1, l2 ->
        (l2.metadata as CMetaData).lineNumberStart!! - (l1.metadata as CMetaData).lineNumberStart!!
    }.forEach { (location, states) ->
        states.mapIndexedNotNull { i, state ->
            state.toAcsl()?.let{ pred -> "@ behavior ${location.name}_b$i: requires $pred;" }
        }.let { preds ->
            if (preds.isNotEmpty()) {
                val lineNumber = location.getCMetaData()!!.lineNumberStart!!.let { lineNumber ->
                    val offset = globalVarDecls?.keys?.count { lineNumber > it } ?: 0
                    if (lineNumber > offset) lineNumber - 1 - offset else 0
                }
                lines.add(lineNumber, preds.joinToString(separator = "\n", prefix = "/*", postfix = "\n@ complete behaviors;*/"))
            }
        }
    }

    // place global variable declarations in the beginning
    val afterExtern = lines.indexOfLast { it.contains("extern") } + 1
    globalVarDecls?.values?.forEachIndexed { i, globalVarDecl -> lines.add(afterExtern + i, globalVarDecl) }

    output.printWriter().use { writer -> lines.forEach(writer::println) }
}

fun<S : State, A : Action> writeAcsl(arg: ARG<S, A>, input: File, output: File) {
    ((arg as? ARG<XcfaState<ExprState>, A>?) ?: TODO("ARG cast not implemented")).writeAcslContracts(input, output)
}

fun main() {
    PredState.of(listOf(
        NotExpr.of(
            IntEqExpr.of(
                RefExpr.of(VarDecl("n", IntType.getInstance())),
                IntLitExpr.of(BigInteger.valueOf(2147483647))
            )
        )
    )).toAcsl()
}
