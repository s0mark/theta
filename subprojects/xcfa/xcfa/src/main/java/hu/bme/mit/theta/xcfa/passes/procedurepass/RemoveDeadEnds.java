package hu.bme.mit.theta.xcfa.passes.procedurepass;

import hu.bme.mit.theta.xcfa.model.XcfaEdge;
import hu.bme.mit.theta.xcfa.model.XcfaLocation;
import hu.bme.mit.theta.xcfa.model.XcfaProcedure;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoveDeadEnds extends ProcedurePass{
	@Override
	public XcfaProcedure.Builder run(XcfaProcedure.Builder builder) {
		XcfaLocation errorLoc = builder.getErrorLoc();
		Set<XcfaEdge> nonDeadEndEdges = new LinkedHashSet<>();
		if(errorLoc != null) {
			collectNonDeadEndEdges(errorLoc, nonDeadEndEdges);
		}
		XcfaLocation finalLoc = builder.getFinalLoc();
		collectNonDeadEndEdges(finalLoc, nonDeadEndEdges);
		Set<XcfaEdge> collect = builder.getEdges().stream().filter(xcfaEdge -> !nonDeadEndEdges.contains(xcfaEdge)).collect(Collectors.toSet());
		for (XcfaEdge edge : collect) {
			builder.removeEdge(edge);
		}
		return builder;
	}

	private void collectNonDeadEndEdges(XcfaLocation loc, Set<XcfaEdge> nonDeadEndEdges) {
		for (XcfaEdge incomingEdge : loc.getIncomingEdges()) {
			if(!nonDeadEndEdges.contains(incomingEdge)) {
				nonDeadEndEdges.add(incomingEdge);
				collectNonDeadEndEdges(incomingEdge.getSource(), nonDeadEndEdges);
			}
		}
	}
}