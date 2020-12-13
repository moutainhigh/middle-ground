package com.cloud.notification.castlers.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorMessage {
    @TableId(type = IdType.ID_WORKER_STR)
    private String id;

    private String message;

    private LocalDateTime createTime = LocalDateTime.now();
}
