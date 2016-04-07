package accounts.test;

import static org.junit.Assert.*;
import org.junit.Test;

import codespecs.PostconditionFailureException;
import codespecs.PreconditionFailureException;
import accounts.Account;
import accounts.BuggyAccount;
import accounts.QEAccount;

public class AccountTest {

	@Test
	public void testAccount() {
		try {
			new Account(-10);
			fail();
		} catch (PreconditionFailureException e) {
		}
		Account a1 = new Account(0);
		assertEquals(0, a1.getBalance());
		try {
			a1.deposit(-10);
			fail();
		} catch (PreconditionFailureException e) {
		}
		a1.deposit(5);
		try {
			a1.withdraw(10);
			fail();
		} catch (PreconditionFailureException e) {
		}
		assertEquals(true, a1.withdraw(3));
		assertEquals(2, a1.getBalance());
	}
	
	@Test
	public void testBuggyAccount() {
		try {
			new BuggyAccount(10);
			fail();
		} catch (PostconditionFailureException e) {
		}
		BuggyAccount a1 = new BuggyAccount(0);
		try {
			a1.deposit(-10);
			fail();
		} catch (PreconditionFailureException e) {
		}
		try {
			a1.deposit(10);
			fail();
		} catch (PostconditionFailureException e) {
		}
		
		BuggyAccount a2 = new BuggyAccount(0);
		try {
			a2.withdraw(0);
			fail();
		} catch (PostconditionFailureException e) {
		}
	}
	
	@Test
	public void testQEAccount() {
		QEAccount a1 = new QEAccount();
		try {
			a1.deposit(-10); // Inherited spec & inherited body.
			fail();
		} catch (PreconditionFailureException e) {
		}
		
		a1.deposit(10);
		assertEquals(true, a1.withdraw(20)); // Would not be allowed by superclass spec.
		assertEquals(10, a1.getBalance());
		
		a1.withdraw(10);
		assertEquals(0, a1.getBalance());
		
		Account a1a = a1;
		try {
			// Test caller-side check.
			a1a.withdraw(10); // Would be allowed by subclass spec; not allowed through type Account.
			fail();
		} catch (PreconditionFailureException e) {
		}
	}

}
