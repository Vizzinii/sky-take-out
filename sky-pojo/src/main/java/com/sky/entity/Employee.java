package com.sky.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @Setter
    @Getter
    private String username;

    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private String password;

    @Setter
    @Getter
    private String phone;

    @Setter
    @Getter
    private String sex;

    @Setter
    @Getter
    private String idNumber;

    @Setter
    @Getter
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Setter
    @Getter
    private LocalDateTime createTime;

    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Getter
    @Setter
    private LocalDateTime updateTime;

    @Getter
    @Setter
    private Long createUser;

    @Setter
    @Getter
    private Long updateUser;


}
