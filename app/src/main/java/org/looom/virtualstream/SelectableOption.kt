package org.looom.virtualstream

/**
 * 配置文件 串流类型/摄像头类型/... 的可选项接口
 */
interface SelectableOption {
    val label: String
    val config: String
}