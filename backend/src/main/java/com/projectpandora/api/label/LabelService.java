package com.projectpandora.api.label;
import com.projectpandora.api.common.ApiException;
import org.springframework.stereotype.Service;
@Service
public class LabelService {
    private final LabelRepository labels;
    public LabelService(LabelRepository labels){this.labels=labels;}
    public WorkLabel resolve(Long id,WorkLabel existing) {
        if(id==null)return existing;
        if(id==0)return null;
        if(existing!=null && id.equals(existing.getId()))return existing;
        return labels.findById(id).filter(WorkLabel::isActive).orElseThrow(()->new ApiException(400,"标签不存在或已停用，请重新选择"));
    }
}
