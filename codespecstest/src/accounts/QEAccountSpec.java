package accounts;

import static codespecs.CodeSpecs.*;

import java.util.function.Consumer;

public class QEAccountSpec {
	public static Consumer<QEAccount> constructorSpec() {
		requires(true);
		return ensures(self -> self.getBalance() == 0);
	}
	
	/**
	 * This spec properly refines the superclass's withdrawSpec:
	 * for cases where the superclass's precondition holds, the postconditions are equivalent.
	 */
	public static Consumer<Boolean> withdrawSpec(QEAccount self, int amount) {
		requires(0 <= amount);
		int oldBalance = self.getBalance();
		return ensures(result ->
			(oldBalance >= amount ?
				self.getBalance() == oldBalance - amount
			:
				self.getBalance() == oldBalance)
			&& result == true
		);
	}
}
