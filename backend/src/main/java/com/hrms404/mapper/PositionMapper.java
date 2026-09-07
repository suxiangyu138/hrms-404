package com.hrms404.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms404.entity.Position;
import com.hrms404.vo.PositionInfoVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 职位 Mapper：CRUD + 职位信息列表（含部门名、在职占用编制）
 */
public interface PositionMapper extends BaseMapper<Position> {

    @Select("""
            SELECT p.position_id, p.position_name, p.dept_id, d.dept_name,
                   p.headcount, p.base_salary,
                   (SELECT COUNT(*) FROM employee e
                     WHERE e.position_id = p.position_id AND e.status = 1) AS used_count
            FROM position p
                     JOIN department d ON p.dept_id = d.dept_id
            ORDER BY p.dept_id, p.position_id
            """)
    List<PositionInfoVO> selectPositionInfos();
}
