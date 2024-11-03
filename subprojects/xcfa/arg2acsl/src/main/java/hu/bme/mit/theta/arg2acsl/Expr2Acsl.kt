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

import hu.bme.mit.theta.core.decl.Decl
import hu.bme.mit.theta.core.type.*
import hu.bme.mit.theta.core.type.abstracttype.*
import hu.bme.mit.theta.core.type.anytype.IteExpr
import hu.bme.mit.theta.core.type.anytype.RefExpr
import hu.bme.mit.theta.core.type.booltype.*
import hu.bme.mit.theta.core.type.inttype.IntLitExpr

private fun<T : Type> Expr<T>.unimplemented(): String
    = TODO("Conversion to ACSL is not implemented for expression $this")

fun<T : Type> NullaryExpr<T>.toAcsl(): String {
    return when (this) {
        is BoolLitExpr -> "\\$this"
        is IntLitExpr -> "$this"
        is RefExpr -> "$this".split("::").last()
        else -> this.unimplemented()
    }
}

fun<OpType : Type, ExprType : Type> UnaryExpr<OpType, ExprType>.toAcsl(): String {
    val op = this.op.toAcsl()
    return when (this) {
        is NotExpr -> "!($op)"
        is PosExpr<*> -> "+($op)"
        is NegExpr<*> -> "-($op)"
        else -> this.unimplemented()
    }
}

fun<OpType: Type, ExprType : Type> BinaryExpr<OpType, ExprType>.toAcsl(): String {
    val leftOp = this.leftOp.toAcsl()
    val rightOp = this.rightOp.toAcsl()
    return when (this) {
        is EqExpr -> "($leftOp) == ($rightOp)"
        is XorExpr -> "($leftOp) ^^ ($rightOp)"
        is NeqExpr -> "($leftOp) != ($rightOp)"
        is GtExpr -> "($leftOp) > ($rightOp)"
        is GeqExpr -> "($leftOp) >= ($rightOp)"
        is LtExpr -> "($leftOp) < ($rightOp)"
        is LeqExpr -> "($leftOp) <= ($rightOp)"
        is SubExpr<*> -> "($leftOp) - ($rightOp)"
        is DivExpr<*> -> "($leftOp) / ($rightOp)"
        is ModExpr<*> -> "($leftOp) % ($rightOp)"
        is RemExpr<*> -> "($leftOp) % ($rightOp)"
        else -> this.unimplemented()
    }
}

fun<OpType : Type, ExprType : Type> MultiaryExpr<OpType, ExprType>.toAcsl(): String {
    val separator = when (this) {
        is AddExpr<*> -> "+"
        is MulExpr<*> -> "*"
        is AndExpr -> "&&"
        is OrExpr -> "||"
        else -> this.unimplemented()
    }
    return this.ops.joinToString(separator = separator) { op ->
        "(${op.toAcsl()})"
    }
}

fun<T : Type> IteExpr<T>.toAcsl(): String
    = "(${this.cond.toAcsl()}) ? (${this.then.toAcsl()}) : (${this.`else`.toAcsl()})"

fun QuantifiedExpr.toAcsl(): String = this.unimplemented()

fun<T : Type> Expr<T>.toAcsl(): String {
    return when (this) {
        is NullaryExpr -> this.toAcsl()
        is UnaryExpr<*, T> -> this.toAcsl()
        is BinaryExpr<*, T> -> this.toAcsl()
        is MultiaryExpr<*, T> -> this.toAcsl()
        is IteExpr<*> -> this.toAcsl()
        is QuantifiedExpr -> this.toAcsl()
        else -> this.unimplemented()
    }
}

fun<T : Type> Decl<T>.toAcsl() = this.name.split("::").last()
