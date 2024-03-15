package com.itheima.stock.security.user;

import com.itheima.stock.vo.resp.PermissionRespNodeVo;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * @author : itheima
 * @date : 2023/2/8 15:46
 * @description  自定义用户详情对象类
 */
@Data
public class LoginUserDetail implements UserDetails {


    private String username;
//    @Override
//    public String getUsername() {
//        return null;
//    }

    private String password;
//    @Override
//    public String getPassword() {
//        return null;
//    }

    private List<GrantedAuthority> authorities;
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return null;
//    }


    /**
     * true:账户没有过期
     */
    private boolean isAccountNonExpired=true;
//    @Override
//    public boolean isAccountNonExpired() {
//        return false;
//    }

    /**
     * true:账户没有锁定
     */
    private boolean isAccountNonLocked=true;
//    @Override
//    public boolean isAccountNonLocked() {
//        return false;
//    }

    /**
     * true:密码没有过期
     */
    private boolean isCredentialsNonExpired=true;
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return false;
//    }
    /**
     * true:账户可用
     */
    private boolean isEnabled=true;
//    @Override
//    public boolean isEnabled() {
//        return false;
//    }

    //其它自定义字段
    /**
     * 用户ID
     */
//    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 电话
     */
    private String phone;
    /**
     * 昵称
     */
    private String nickName;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 性别(1.男 2.女)
     */
    private Integer sex;

    /**
     * 账户状态(1.正常 2.锁定 )
     */
    private Integer status;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 权限树，不包含按钮相关权限信息
     */
    private List<PermissionRespNodeVo> menus;

    /**
     * 前端按钮权限集合
     */
    private List<String> permissions;


}
