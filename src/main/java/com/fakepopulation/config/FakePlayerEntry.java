package com.fakepopulation.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class FakePlayerEntry extends OkaeriConfig {

    @Comment("假玩家名字")
    @CustomKey("name")
    private String name = "";

    @Comment("是否计入在线人数")
    @CustomKey("enabled")
    private boolean enabled = true;
}