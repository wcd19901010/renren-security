package io.renren.modules.sys.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;
import io.renren.modules.sys.dao.CityInfoDao;
import io.renren.modules.sys.dao.CourierDao;
import io.renren.modules.sys.entity.CourierEntity;
import io.renren.modules.sys.service.CourierService;
import io.renren.modules.sys.vo.CourierVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;


@SuppressWarnings("ALL")
@Service("courierService")
public class CourierServiceImpl extends ServiceImpl<CourierDao, CourierEntity> implements CourierService {

    @Autowired
    private CityInfoDao cityInfoDao;
    @Autowired
    private CourierDao courierDao;


    private static List<Object> templetList = CollUtil.newArrayList();

    static {
        // 片区 城市 站点 erp账号 姓名 身份证号码 电话 银行卡号 开户行 联行号 入职时间 离职时间 状态 备注
        templetList.add("片区");
        templetList.add("城市");
        templetList.add("站点");
        templetList.add("erp账号");
        templetList.add("姓名");
        templetList.add("身份证号码");
        templetList.add("电话");
        templetList.add("银行卡号");
        templetList.add("开户行");
        templetList.add("联行号");
        templetList.add("入职时间");
        templetList.add("离职时间");
        templetList.add("状态");
        templetList.add("备注");
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Page<CourierEntity> page = this.selectPage(
                new Query<CourierEntity>(params).getPage(),
                new EntityWrapper<CourierEntity>()
        );

        return new PageUtils(page);
    }


    public PageUtils selectMyPage(Map<String, Object> params) {
        Page<CourierVo> page = new Query<CourierVo>(params).getPage();
        CourierVo courierVo = BeanUtil.mapToBean(params, CourierVo.class, true);
        page.setRecords(courierDao.selectMyPage(page, courierVo));
        return new PageUtils(page);
    }

    /**
     * 上传文件
     *
     * @param multipartFile
     */
    @Override
    public void importCourier(CommonsMultipartFile multipartFile) {
        try {
            InputStream is = multipartFile.getInputStream();
            String filename = multipartFile.getOriginalFilename();
            ExcelReader reader = ExcelUtil.getReader(is, 0);
            // TODO: 2018/12/15 判断是否为规定模板
            List<List<Object>> readFirstList = reader.read(0, 0);
            List<Object> columnList = readFirstList.get(0);
            if (isTemplet(columnList, templetList)) {
                // TODO: 2018/12/15 读取文件
                List<List<Object>> contentList = reader.read(1);
                // TODO: 2018/12/15 解析数据
                List<CourierEntity> courierList = CollUtil.newArrayList();
                contentList.forEach(lineList -> {
                    CourierEntity courierEntity = new CourierEntity();
                    // 片区 城市 站点 erp账号 姓名 身份证号码 电话 银行卡号 开户行 联行号 入职时间 离职时间 状态 备注
                    courierEntity.setArea(Convert.toStr(lineList.get(0)));
                    // 根据城市名称获取城市id
                    Integer cityId = cityInfoDao.getIdByCityName(lineList.get(1));
                    courierEntity.setCityId(cityId);
                    courierEntity.setSite(Convert.toStr(lineList.get(2)));
                    courierEntity.setErpId(Convert.toStr(lineList.get(3)));
                    courierEntity.setCourierName(Convert.toStr(lineList.get(4)));
                    courierEntity.setCardId(Convert.toStr(lineList.get(5)));
                    courierEntity.setPhone(Convert.toStr(lineList.get(6)));
                    courierEntity.setBankCardId(Convert.toStr(lineList.get(7)));
                    courierEntity.setDepositBank(Convert.toStr(lineList.get(8)));
                    courierEntity.setJoinBankNumber(Convert.toStr(lineList.get(9)));
                    courierEntity.setEntryDate(Convert.toDate(lineList.get(10)));
                    courierEntity.setLeaveDate(Convert.toDate(lineList.get(11)));
                    int status = 0;
                    if (Convert.toStr(lineList.get(12)).equals("已绑定第三方")) {
                        status = 1;
                    }
                    courierEntity.setStatus(Convert.toInt(status));
                    courierEntity.setComment(Convert.toStr(lineList.get(13)));
                    courierList.add(courierEntity);
                });
                // TODO: 2018/12/15 存入数据库
                this.insertBatch(courierList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 是否为Excel模板
     *
     * @param columnList
     * @param templetList
     * @return
     */
    private boolean isTemplet(List<Object> columnList, List<Object> templetList) {
        int templetSize = templetList.size();
        int colSize = columnList.size();
        if (templetSize == colSize) {
            int count = 0;
            for (int i = 0; i < colSize; i++) {
                if (templetList.get(i).equals(columnList.get(i))) {
                    count++;
                }
            }
            if (count == templetSize) {
                return true;
            }
        }
        return false;
    }
}
