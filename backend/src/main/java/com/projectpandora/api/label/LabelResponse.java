package com.projectpandora.api.label;
/** Resolve persistence proxies inside the transaction; never serialize an entity association. */
public record LabelResponse(Long id,String name,String color,boolean active) {
    public static LabelResponse from(WorkLabel label){return label==null?null:new LabelResponse(label.getId(),label.getName(),label.getColor(),label.isActive());}
}
