package accounts;

import codespecs.SeeCodeSpecs;

@SeeCodeSpecs
public class QEAccount extends Account {
	public QEAccount() {
		super(0);
	}
	
	public boolean withdraw(int amount) {
		if (getBalance() >= amount) {
			super.withdraw(amount);
		}
		return true;
	}
}
