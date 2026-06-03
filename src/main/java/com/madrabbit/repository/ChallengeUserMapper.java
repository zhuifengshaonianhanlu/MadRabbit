package com.madrabbit.repository;

import com.madrabbit.entity.ChallengeUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChallengeUserMapper {
    List<ChallengeUser> searchByAge(@Param("age") String age);
    List<ChallengeUser> searchByName(@Param("name") String name);
    List<ChallengeUser> searchByNameLike(@Param("name") String name);
    List<Map<String, Object>> searchLogs(@Param("tableName") String tableName);
    List<ChallengeUser> listUsersOrderBy(@Param("columnName") String columnName);
    List<ChallengeUser> searchByNameSafeWithOrder(@Param("name") String name, @Param("orderColumn") String orderColumn, @Param("orderDir") String orderDir);
    List<ChallengeUser> searchByIds(@Param("ids") String ids);
    // 安全搜索(#{}) + 不安全角色筛选(${}注入)
    List<ChallengeUser> searchByNameSafeWithRoles(@Param("name") String name, @Param("roles") String roles);
    // 盲注关卡 - 字符型盲注
    List<ChallengeUser> searchByNameBlind(@Param("name") String name);
}
