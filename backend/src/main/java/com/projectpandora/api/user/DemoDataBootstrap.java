package com.projectpandora.api.user;

import com.projectpandora.api.notice.CompanyNoticeEntity;
import com.projectpandora.api.notice.CompanyNoticeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a richer demo organization (multiple departments, team leads and staff) and the
 * company top-10 notices. Idempotent: existing accounts/notices are left untouched so
 * administrator edits are preserved. Excluded from the test profile.
 */
@Component
@Order(2)
@Profile("!test")
public class DemoDataBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final CompanyNoticeRepository notices;
    private final PasswordEncoder passwords;

    public DemoDataBootstrap(UserRepository users,CompanyNoticeRepository notices,PasswordEncoder passwords) {
        this.users=users;this.notices=notices;this.passwords=passwords;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash=passwords.encode("demo1234");
        var founder=ensure("founder",hash,Role.FOUNDER,"创始人",null);
        ensure("admin_ops",hash,Role.ADMIN,"运营管理员",null);

        var leader=ensure("leader",hash,Role.DEPT_HEAD,"部门老总",founder);
        var deptTech=ensure("dept_tech",hash,Role.DEPT_HEAD,"技术部老总",founder);
        var deptMarket=ensure("dept_market",hash,Role.DEPT_HEAD,"市场部老总",founder);
        var deptOps=ensure("dept_ops",hash,Role.DEPT_HEAD,"运营部老总",founder);

        var teamLead=ensure("team_lead",hash,Role.TEAM_LEAD,"团队长",leader);
        var teamLead2=ensure("team_lead_2",hash,Role.TEAM_LEAD,"综合组组长",leader);
        var leadBackend=ensure("lead_backend",hash,Role.TEAM_LEAD,"后端组长",deptTech);
        var leadFrontend=ensure("lead_frontend",hash,Role.TEAM_LEAD,"前端组长",deptTech);
        var leadSales=ensure("lead_sales",hash,Role.TEAM_LEAD,"销售组长",deptMarket);
        var leadBrand=ensure("lead_brand",hash,Role.TEAM_LEAD,"品牌组长",deptMarket);
        var leadService=ensure("lead_service",hash,Role.TEAM_LEAD,"客服组长",deptOps);

        ensure("staff",hash,Role.STAFF,"员工小王",teamLead);
        ensure("staff_2",hash,Role.STAFF,"李雷",teamLead);
        ensure("staff_3",hash,Role.STAFF,"韩梅梅",teamLead);
        ensure("staff_4",hash,Role.STAFF,"张伟",teamLead2);
        ensure("staff_5",hash,Role.STAFF,"王芳",teamLead2);
        ensure("dev_1",hash,Role.STAFF,"赵强",leadBackend);
        ensure("dev_2",hash,Role.STAFF,"陈晨",leadBackend);
        ensure("dev_3",hash,Role.STAFF,"周涛",leadBackend);
        ensure("fe_1",hash,Role.STAFF,"林静",leadFrontend);
        ensure("fe_2",hash,Role.STAFF,"何磊",leadFrontend);
        ensure("sales_1",hash,Role.STAFF,"孙悦",leadSales);
        ensure("sales_2",hash,Role.STAFF,"郑浩",leadSales);
        ensure("sales_3",hash,Role.STAFF,"冯雪",leadSales);
        ensure("brand_1",hash,Role.STAFF,"许晴",leadBrand);
        ensure("brand_2",hash,Role.STAFF,"邓超",leadBrand);
        ensure("cs_1",hash,Role.STAFF,"曹阳",leadService);
        ensure("cs_2",hash,Role.STAFF,"彭丽",leadService);
        ensure("cs_3",hash,Role.STAFF,"唐磊",leadService);

        seedNotices(founder.getId());
    }

    private UserEntity ensure(String username,String hash,Role role,String displayName,UserEntity manager) {
        var existing=users.findByUsername(username);
        if(existing.isPresent()) return existing.get();
        var user=new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(hash);
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setManagerId(manager==null?null:manager.getId());
        return users.saveAndFlush(user);
    }

    private void seedNotices(Long publisherId) {
        String[][] data={
            {"年度营收突破 1 亿元","全年营收首次突破 1 亿元，同比增长 42%，创历史新高。"},
            {"新一代产品平台正式发布","Pandora 3.0 平台上线，统一了任务、日志与审批流程，覆盖全公司业务。"},
            {"完成 A 轮融资","获得头部机构 A 轮融资，资金将用于产品研发与市场拓展。"},
            {"华东区域中心成立","华东区域中心在上海挂牌，正式覆盖长三角客户服务。"},
            {"通过 ISO 9001 质量体系认证","公司质量管理体系通过 ISO 9001 认证，交付质量再上台阶。"},
            {"团队规模突破 100 人","核心团队扩充至 100 人，研发人员占比超过 50%。"},
            {"与头部客户达成战略合作","与三家行业头部客户签署年度战略合作协议，锁定长期订单。"},
            {"自研协同系统全面上线","自研的协同办公系统在全公司范围正式上线，流程线上化率达到 95%。"},
            {"员工满意度创新高","年度员工满意度调查得分 92 分，创历史新高。"},
            {"启动海外市场拓展","正式启动东南亚市场拓展计划，首站落地新加坡。"}
        };
        int order=1;
        for(String[] row:data) {
            if(!notices.existsByTitle(row[0])) {
                var notice=new CompanyNoticeEntity();
                notice.setTitle(row[0]);
                notice.setContent(row[1]);
                notice.setSortOrder(order);
                notice.setEnabled(true);
                notice.setPublisherId(publisherId);
                notices.save(notice);
            }
            order++;
        }
    }
}
