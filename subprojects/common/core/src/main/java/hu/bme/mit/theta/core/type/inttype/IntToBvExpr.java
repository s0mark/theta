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

package hu.bme.mit.theta.core.type.inttype;

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.abstracttype.CastExpr;
import hu.bme.mit.theta.core.type.bvtype.BvLitExpr;
import hu.bme.mit.theta.core.type.bvtype.BvType;

import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class IntToBvExpr extends CastExpr<IntType, BvType> {
  private static final int HASH_SEED = 6873;
  private static final String OPERATOR_LABEL = "int_to_bv";

  private IntToBvExpr(final Expr<IntType> op) {
    super(op);
  }

  public static IntToBvExpr of(final Expr<IntType> op) {
    return new IntToBvExpr(op);
  }

  public static IntToBvExpr create(final Expr<?> op) {
    final Expr<IntType> newOp = cast(op, Int());
    return IntToBvExpr.of(newOp);
  }

  public int getSize() {
    return 32; // TODO infer size
  }

  @Override
  public BvType getType() {
    return BvType.of(getSize());
  }

  @Override
  public BvLitExpr eval(final Valuation val) {
    final IntLitExpr opVal = (IntLitExpr) getOp().eval(val);
    return opVal.toBv();
  }

  @Override
  public IntToBvExpr with(final Expr<IntType> op) {
    if (op == getOp()) {
      return this;
    } else {
      return IntToBvExpr.of(op);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (obj != null && this.getClass() == obj.getClass()) {
      final IntToBvExpr that = (IntToBvExpr) obj;
      return this.getOp().equals(that.getOp());
    } else {
      return false;
    }
  }

  @Override
  protected int getHashSeed() {
    return HASH_SEED;
  }

  @Override
  public String getOperatorLabel() {
    return OPERATOR_LABEL;
  }
}
