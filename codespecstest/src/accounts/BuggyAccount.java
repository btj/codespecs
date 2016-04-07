package accounts;

import codespecs.SeeCodeSpecs;

@SeeCodeSpecs
public class BuggyAccount {
	private int balance;
	
	public BuggyAccount(int initialBalance) {
		balance = initialBalance / 2;
	}
	
	public int getBalance() {
		return balance;
	}
	
	public void deposit(int amount) {
		balance -= amount;
	}
	
	public boolean withdraw(int amount) {
		balance += 10;
		return true;
	}
}
