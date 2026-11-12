package com.projectpandora.api.label;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.common.ApiException;
@Service public class ContentLabelStore {
 private final JdbcTemplate db;private final LabelRepository labels;private final ObjectMapper json;
 public ContentLabelStore(JdbcTemplate db,LabelRepository labels,ObjectMapper json){this.db=db;this.labels=labels;this.json=json;}
 public void save(String kind,Long id,List<Long> values) {
  if(values==null)return;
  if(values.size()>12 || values.contains(null))throw new ApiException(400,"最多选择12个有效标签");
  var ids=values.stream().distinct().toList();
  var previous=existingIds(kind,id);
  for(Long value:ids)
   if(labels.findById(value).filter(label->label.isActive() || previous.contains(value)).isEmpty())
    throw new ApiException(400,"标签不存在或已停用");
  try {
   String text=json.writeValueAsString(ids);
   db.update("delete from content_label_sets where kind=? and content_id=?",kind,id);
   db.update("insert into content_label_sets(kind,content_id,label_ids) values(?,?,?)",kind,id,text);
  } catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new ApiException(500,"标签保存失败");}
 }
 private Set<Long> existingIds(String kind,Long id) {
  var records=db.queryForList("select label_ids from content_label_sets where kind=? and content_id=?",String.class,kind,id);
  if(!records.isEmpty()) {
   try {return new HashSet<>(Arrays.asList(json.readValue(records.get(0),Long[].class)));}
   catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new ApiException(500,"标签记录读取失败");}
  }
  // Records created before multi-label support keep their existing single label.
  String table=switch(kind) {case "task"->"dispatch_tasks";case "log"->"work_logs";default->null;};
  if(table==null)return Set.of();
  return new HashSet<>(db.queryForList("select label_id from "+table+" where id=? and label_id is not null",Long.class,id));
 }
}
