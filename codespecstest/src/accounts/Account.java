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
