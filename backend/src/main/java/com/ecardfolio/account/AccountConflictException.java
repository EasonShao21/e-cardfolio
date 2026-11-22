package com.ecardfolio.account;

public class AccountConflictException extends RuntimeException {
    public AccountConflictException() { super("此 Email 已註冊，請直接登入。"); }
}
