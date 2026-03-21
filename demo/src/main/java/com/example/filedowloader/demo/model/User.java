package com.example.filedowloader.demo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;

@Entity
@Table(name="app_user")
public class User implements UserDetails{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name="user_seq", sequenceName = "user_req", allocationSize = 1)
    private Long id;

    @Column(name= "user_name", unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<DownloadTask> downloads= new ArrayList<>();

    protected User(){}

    public User(String username, String password, Role role){
        this.username=username;
        this.password= password;
        this.role= role;
    }

    public Long getId(){
        return id;
    }

    public String getUserName(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    public Role getRole(){
        return role;
    }

    //Now the SPRING SECURITY PART STARTS

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){

        return List.of(new SimpleGrantedAuthority("ROLE_"+this.role.name()));
    }
    
    @Override
    public String getUsername(){
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    @Override
    public boolean isAccountNonLocked(){
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    @Override
    public boolean isEnabled(){
        return true;
    }
}
