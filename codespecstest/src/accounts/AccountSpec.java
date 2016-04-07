package accounts;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static codespecs.CodeSpecs.*;

public class AccountSpec {
	public static Consumer<Account> constructorSpec(int initialBalance) {
		requires(0 <= initialBalance);
		return ensures(self -> self.getBalance() == initialBalance);
	}
	
	public static Runnable depositSpec(Account self, int amount) {
		requires(0 <= amount);
		int oldBalance = self.getBalance();
		return ensures(() -> self.getBalance() == oldBalance + amount);
	}
	
	public static Consumer<Boolean> withdrawSpec(Account self, int amount) {
		requires(0 <= amount && self.getBalance() >= amount);
		int oldBalance = self.getBalance();
		return ensures(result -> self.getBalance() == oldBalance - amount && result == true);
	}
}
