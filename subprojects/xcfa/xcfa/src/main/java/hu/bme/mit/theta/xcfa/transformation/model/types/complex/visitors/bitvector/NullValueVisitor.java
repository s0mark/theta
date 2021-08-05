package hu.bme.mit.theta.xcfa.transformation.model.types.complex.visitors.bitvector;

import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.fptype.FpType;
import hu.bme.mit.theta.core.utils.BvUtils;
import hu.bme.mit.theta.core.utils.FpUtils;
import hu.bme.mit.theta.xcfa.transformation.ArchitectureConfig;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.CComplexType;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.CInteger;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.Signed;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.real.CDouble;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.real.CFloat;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.real.CLongDouble;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;

import java.math.BigInteger;

public class NullValueVisitor extends CComplexType.CComplexTypeVisitor<Void, LitExpr<?>> {
	public static final NullValueVisitor instance = new NullValueVisitor();

	@Override
	public LitExpr<?> visit(CDouble type, Void param) {
		return FpUtils.bigFloatToFpLitExpr(
				new BigFloat(
						"0.0",
						new BinaryMathContext(
								ArchitectureConfig.architecture.getBitWidth("double_e"),
								ArchitectureConfig.architecture.getBitWidth("double_s"))),
				FpType.of(
						ArchitectureConfig.architecture.getBitWidth("double_e"),
						ArchitectureConfig.architecture.getBitWidth("double_s")));
	}

	@Override
	public LitExpr<?> visit(CFloat type, Void param) {
		return FpUtils.bigFloatToFpLitExpr(
				new BigFloat(
						"0.0",
						new BinaryMathContext(
								ArchitectureConfig.architecture.getBitWidth("float_e"),
								ArchitectureConfig.architecture.getBitWidth("float_s"))),
				FpType.of(
						ArchitectureConfig.architecture.getBitWidth("float_e"),
						ArchitectureConfig.architecture.getBitWidth("float_s")));
	}

	@Override
	public LitExpr<?> visit(CLongDouble type, Void param) {
		return FpUtils.bigFloatToFpLitExpr(
				new BigFloat(
						"0.0",
						new BinaryMathContext(
								ArchitectureConfig.architecture.getBitWidth("longdouble_e"),
								ArchitectureConfig.architecture.getBitWidth("longdouble_s"))),
				FpType.of(
						ArchitectureConfig.architecture.getBitWidth("longdouble_e"),
						ArchitectureConfig.architecture.getBitWidth("longdouble_s")));
	}

	@Override
	public LitExpr<?> visit(CInteger type, Void param) {
		if(type instanceof Signed) {
			return BvUtils.bigIntegerToSignedBvLitExpr(BigInteger.ZERO, type.width());
		} else {
			return BvUtils.bigIntegerToUnsignedBvLitExpr(BigInteger.ZERO, type.width());
		}
	}
}