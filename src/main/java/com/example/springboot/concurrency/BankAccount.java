package com.example.springboot.concurrency;

/**
 * Classic interview problem: a thread-safe account.
 *
 * Amounts are in MINOR UNITS (cents) as a long. Integer cents are the standard way to avoid
 * floating-point money bugs (0.1 + 0.2 != 0.3 with double). BigDecimal is the alternative
 * when you need fractional cents, currency conversion or explicit rounding modes.
 * Never use double/float for money.
 *
 * Without synchronized, two races appear:
 *  1. Lost update: "balance += amount" is read-modify-write (3 steps), so two threads
 *     can read the same old value and one deposit disappears.
 *  2. Check-then-act: two threads both see balance >= amount, both withdraw,
 *     and the balance goes negative.
 *
 * synchronized gives mutual exclusion (one thread at a time per account) AND
 * visibility (writes are seen by the next thread that takes the lock).
 *
 * Why not AtomicLong? It fixes (1) via addAndGet, but (2) needs a compareAndSet
 * retry loop. For multi-step invariants, a lock is simpler and clearer.
 */
public class BankAccount {

    private long balance;

    public BankAccount(long initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("initial balance must be >= 0");
        }
        this.balance = initialBalance;
    }

    /**
     * Math.addExact throws ArithmeticException on overflow. Plain "+=" would silently wrap
     * Long.MAX_VALUE + 1 to a huge NEGATIVE balance. Because it throws before the assignment,
     * the balance is left unchanged.
     */
    public synchronized void deposit(long amount) {
        requirePositive(amount);
        balance = Math.addExact(balance, amount);
    }

    /** @return true if withdrawn, false if insufficient funds (balance never goes negative) */
    public synchronized boolean withdraw(long amount) {
        requirePositive(amount);
        if (balance < amount) {
            return false;
        }
        // Cannot overflow (0 < amount <= balance), but subtractExact documents the intent for free.
        balance = Math.subtractExact(balance, amount);
        return true;
    }

    // Reads are synchronized too: otherwise a thread may see a stale value (visibility).
    public synchronized long balance() {
        return balance;
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }
}
