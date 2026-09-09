package cn.edu.csu.plane.controller;

import cn.edu.csu.plane.model.Enemy;
import cn.edu.csu.plane.model.Item;

/**
 * 生成管理：按难度与关卡控制敌机、道具的生成节奏与类型。
 */
public class SpawnManager {

    public void update(double deltaTime, int level) {
        // TODO: 根据难度与关卡控制生成节奏
    }

    public Enemy createEnemy() {
        // TODO: 根据难度随机类型生成敌机
        return null;
    }

    public Item createItem() {
        // TODO: 按概率生成道具
        return null;
    }
}
