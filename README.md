# Code Specs for Java

This project defines a convention for indicating in a Java program text that a particular method serves as a specification for another method, and for interpreting such a specification method as a precondition and a postcondition for the specified method. It also provides a Java agent that inserts bytecode at class load time that checks at calls of specified methods at run time that the precondition and postcondition hold.

For example, consider the following class:

```java
package accounts;

import codespecs.SeeCodeSpecs;

@SeeCodeSpecs
public class Account {
	private int balance;
	
	public Account(int initialBalance) {
		balance = initialBalance;
	}
	
	public int getBalance() {
		return balance;
	}
	
	public void deposit(int amount) {
		balance += amount;
	}
	
	public boolean withdraw(int amount) {
		balance -= amount;
		return true;
	}
}
```

The `@SeeCodeSpecs` annotation indicates that specifications for class `Account` are provided in class `AccountSpec`, defined as follows:

```java
package accounts;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static codespecs.CodeSpecs.requires;
import static codespecs.CodeSpecs.ensures;

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
```

Notice that for each constructor and method of class `Account`, there is a corresponding static method in class `AccountSpec`.

Suppose we compile the program containing classes `Account` and `AccountSpec` into `accounttest.jar`. To cause the specifications to be checked at each call at run time, use the following command line:

```
java -javaagent:codespecsweaver.jar -jar accounttest.jar
```
