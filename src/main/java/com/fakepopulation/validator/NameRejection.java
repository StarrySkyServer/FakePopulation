package com.fakepopulation.validator;

public enum NameRejection {

    EMPTY("名字不能为空"),
    LENGTH("名字长度必须在 3-15 个字符之间"),
    CHARSET("名字只能包含字母、数字、空格、撇号及拉丁/CJK 字符"),
    SPACING("名字首尾不能有空格，也不能有连续空格"),
    ONLINE("该玩家正在线，不能作为假玩家"),
    REGISTERED("该名字已被玩家注册过，不能作为假玩家"),
    DUPLICATE("名单中已存在该名字"),
    RESERVED("该名字在保留名单中"),
    LIMIT("假玩家数量已达上限");

    private final String message;

    NameRejection(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}