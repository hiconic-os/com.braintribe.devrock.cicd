// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package devrock.cicd.steps.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.braintribe.devrock.mc.api.transitive.BuildRange;
import com.braintribe.devrock.mc.api.transitive.RangedTerminals;
import com.braintribe.devrock.mc.core.resolver.transitive.BoundaryComparator;
import com.braintribe.devrock.mc.core.resolver.transitive.BoundaryFloorComparator;
import com.braintribe.devrock.mc.core.resolver.transitive.DisjunctionBoundaryComparator;
import com.braintribe.devrock.model.mc.reason.InvalidRepositoryConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.compiled.CompiledTerminal;

public interface RangeParser {

	static Maybe<RangedTerminals> parse(String concatString, String groupId, String groupVersion) {
		DisjunctionBoundaryComparator lowerBound = new DisjunctionBoundaryComparator();
		DisjunctionBoundaryComparator upperBound = new DisjunctionBoundaryComparator();
		List<CompiledDependencyIdentification> terminals = new ArrayList<>();
		
		String boundsAsString[] = concatString.split("\\+");
		
		
		for (String boundaryAsStr: boundsAsString) {
			boundaryAsStr = boundaryAsStr.trim();
			if (boundaryAsStr.isEmpty())
				return Reasons.build(InvalidRepositoryConfiguration.T).text("illegal build range boundary [" + boundaryAsStr + "]").toMaybe();
			
			char firstChar = boundaryAsStr.charAt(0);
			char lastChar = boundaryAsStr.charAt(boundaryAsStr.length() - 1);
			int s = 0, e = boundaryAsStr.length();
			
			List<Consumer<CompiledDependencyIdentification>> consumers = new ArrayList<>(2);

			boolean noLowerBracket = false;
			 
			switch (firstChar) {
			case '(':
			case ']':
				consumers.add(d -> lowerBound.addOperand(new BoundaryComparator(d, true)));
				s++;
				break;
			case '[':
				consumers.add(d -> lowerBound.addOperand(new BoundaryComparator(d, false)));
				s++;
				break;
			default:
				noLowerBracket = true;
				break;
			}
			
			switch (lastChar) {
				case ')':
				case '[':
					consumers.add(d -> upperBound.addOperand(new BoundaryComparator(d, true)));
					e--;
					break;
				case ']':
					consumers.add(d -> upperBound.addOperand(new BoundaryComparator(d, false)));
					e--;
					break;
				default:
					if (noLowerBracket) {
						consumers.add(d -> upperBound.addOperand(new BoundaryComparator(d, false)));
					}
					break;
			}
			
			String dependencyAsStr = groupId + ":" + boundaryAsStr.substring(s, e) + "#" + groupVersion;
			
			CompiledDependencyIdentification dependency = CompiledDependencyIdentification.parseAndRangify(dependencyAsStr, true);
			
			consumers.forEach(c -> c.accept(dependency));
			
			terminals.add(dependency);
		}
		
		if (lowerBound.isEmpty()) {
			lowerBound.addOperand(BoundaryFloorComparator.INSTANCE);
		}
		
		BuildRange range = BuildRange.of(lowerBound, upperBound);

		return Maybe.complete(new RangedTerminals() {
			
			@Override
			public Iterable<? extends CompiledTerminal> terminals() {
				return terminals;
			}
			
			@Override
			public BuildRange range() {
				return range;
			}
		});
	}
}
