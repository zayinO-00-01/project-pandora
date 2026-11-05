package com.projectpandora.api.label;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.common.ApiException;
@Service public class ContentLabelStore {
 private final JdbcTemplate db;private final LabelRepository labels;private final ObjectMapper json;
 public ContentLabelStore(JdbcTemplate db,LabelRepository labels,ObjectMapper json){this.db=db;this.labels=labels;this.json=json;}
 public void save(String kind,Long id,List<Long> values){if(values==null)return;if(values.size()>12 || values.contains(null))throw new ApiException(400,"最多选择12个有效标签");var ids=values.stream().distinct().toList();for(Long v:ids)if(labels.findById(v).filter(WorkLabel::isActive).isEmpty())throw new ApiException(400,"标签不存在或已停用");try{String text=json.writeValueAsString(ids);db.update("delete from content_label_sets where kind=? and content_id=?",kind,id);db.update("insert into content_label_sets(kind,content_id,label_ids) values(?,?,?)",kind,id,text);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new ApiException(500,"标签保存失败");}}
}
