package codespecs;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CodeSpecs {
	public static void requires(boolean condition) {
		if (!condition)
			throw new PreconditionFailureException("Precondition does not hold");
	}
	
	public static <T> Consumer<T> ensures(Predicate<T> condition) {
		return self -> {
			if (!condition.test(self))
				throw new PostconditionFailureException("Postcondition does not hold");
		};
	}
	
	public static Runnable ensures(BooleanSupplier condition) {
		return () -> {
			if (!condition.getAsBoolean())
				throw new PostconditionFailureException("Postcondition does not hold");
		};
	}
}
