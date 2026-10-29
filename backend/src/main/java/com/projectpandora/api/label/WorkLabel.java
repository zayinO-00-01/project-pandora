package com.projectpandora.api.label;
import jakarta.persistence.*;
@Entity @Table(name="work_labels")
public class WorkLabel {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=64) private String name;
    @Column(nullable=false,length=7) private String color;
    @Column(nullable=false) private boolean active=true;
    public Long getId(){return id;}
    public String getName(){return name;}
    public String getColor(){return color;}
    public boolean isActive(){return active;}
    public void setName(String value){name=value;}
    public void setColor(String value){color=value;}
    public void setActive(boolean value){active=value;}
}
