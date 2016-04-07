package accounts;

import java.util.function.Consumer;

import static codespecs.CodeSpecs.*;

public class BuggyAccountSpec {
	public static Consumer<BuggyAccount> constructorSpec(int initialBalance) {
		requires(0 <= initialBalance);
		return ensures(self -> self.getBalance() == initialBalance);
	}
	
	public static Runnable depositSpec(BuggyAccount self, int amount) {
		requires(0 <= amount);
		int oldBalance = self.getBalance();
		return ensures(() -> self.getBalance() == oldBalance + amount);
	}
	
	public static Consumer<Boolean> withdrawSpec(BuggyAccount self, int amount) {
		requires(0 <= amount && self.getBalance() >= amount);
		int oldBalance = self.getBalance();
		return ensures(result -> self.getBalance() == oldBalance - amount && result == true);
	}
}
