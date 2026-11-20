-- 诊断：查看 complainant_phone 为 NULL 的投诉记录详情
-- ★create_by 是用户名（String），用 user_name 匹配
SELECT cr.id, cr.create_by, cr.complainant_phone, cr.complainant_name,
       cr.complaint_channel, cr.content_text, cr.tenant_id,
       u.user_id, u.user_name, u.phonenumber, u.nick_name
  FROM complaint_record cr
  LEFT JOIN sys_user u ON u.user_name = cr.create_by
 WHERE cr.complainant_phone IS NULL;
