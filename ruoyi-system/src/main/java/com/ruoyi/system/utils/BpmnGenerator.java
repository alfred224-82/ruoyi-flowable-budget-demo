package com.ruoyi.system.utils;

import com.ruoyi.system.domain.FeeWorkflowNodeConfig;

import java.util.List;

/**
 * 根据工作流节点配置动态生成 BPMN 2.0 XML
 *
 * @author ruoyi
 * @date 2026-07-24
 */
public class BpmnGenerator {

    private BpmnGenerator() {
    }

    /**
     * 生成审批流程 BPMN XML
     *
     * @param processKey  流程定义Key（如 budget_preparation）
     * @param processName 流程名称（如 预算编制工作流）
     * @param nodes       审批节点列表（按 nodeSeq 排序）
     * @return BPMN 2.0 XML 字符串
     */
    public static String generateApprovalProcess(String processKey, String processName,
                                                  List<FeeWorkflowNodeConfig> nodes) {
        StringBuilder xml = new StringBuilder();

        // XML头 + definitions
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n");
        xml.append("             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("             xmlns:flowable=\"http://flowable.org/bpmn\"\n");
        xml.append("             targetNamespace=\"http://flowable.org/bpmn\">\n");
        xml.append("  <process id=\"").append(processKey).append("\" name=\"").append(processName)
            .append("\" isExecutable=\"true\">\n");

        // 开始事件
        xml.append("    <startEvent id=\"startEvent\" name=\"开始\"/>\n");

        // 开始事件到第一个节点的连线
        xml.append("    <sequenceFlow id=\"flow_start\" sourceRef=\"startEvent\" targetRef=\"node_1\"/>\n");

        // 遍历节点生成 userTask + gateway + 连线
        for (int i = 0; i < nodes.size(); i++) {
            FeeWorkflowNodeConfig node = nodes.get(i);
            int seq = node.getNodeSeq();
            String nodeId = "node_" + seq;
            String gatewayId = "gateway_" + seq;

            // UserTask
            xml.append("    <userTask id=\"").append(nodeId).append("\" name=\"").append(node.getNodeName()).append("\"");
            xml.append(" flowable:candidateGroups=\"ROLE_").append(node.getRoleId()).append("\">\n");

            // 会签多实例配置
            if ("ALL".equals(node.getApprovalType())) {
                xml.append("      <multiInstanceLoopCharacteristics isSequential=\"false\"\n");
                xml.append("          flowable:collection=\"assigneeList\"\n");
                xml.append("          flowable:elementVariable=\"assignee\">\n");
                xml.append("        <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>\n");
                xml.append("      </multiInstanceLoopCharacteristics>\n");
            }
            xml.append("    </userTask>\n");

            // 节点到网关的连线
            xml.append("    <sequenceFlow id=\"flow_").append(nodeId).append("_to_").append(gatewayId)
                .append("\" sourceRef=\"").append(nodeId).append("\" targetRef=\"").append(gatewayId).append("\"/>\n");

            // 排他网关
            xml.append("    <exclusiveGateway id=\"").append(gatewayId).append("\" name=\"审批结果\"/>\n");

            // 网关到下一节点或通过连线的条件
            if (i < nodes.size() - 1) {
                int nextSeq = nodes.get(i + 1).getNodeSeq();
                String nextNodeId = "node_" + nextSeq;
                // 通过 → 下一节点
                xml.append("    <sequenceFlow id=\"flow_").append(gatewayId).append("_pass\" sourceRef=\"")
                    .append(gatewayId).append("\" targetRef=\"").append(nextNodeId).append("\">\n");
                xml.append("      <conditionExpression xsi:type=\"tFormalExpression\">${result == 'pass'}</conditionExpression>\n");
                xml.append("    </sequenceFlow>\n");
            } else {
                // 最后一个节点通过 → 审批通过结束事件
                xml.append("    <sequenceFlow id=\"flow_").append(gatewayId).append("_pass\" sourceRef=\"")
                    .append(gatewayId).append("\" targetRef=\"endEventApproved\">\n");
                xml.append("      <conditionExpression xsi:type=\"tFormalExpression\">${result == 'pass'}</conditionExpression>\n");
                xml.append("    </sequenceFlow>\n");
            }

            // 驳回 → 审批驳回结束事件
            xml.append("    <sequenceFlow id=\"flow_").append(gatewayId).append("_reject\" sourceRef=\"")
                .append(gatewayId).append("\" targetRef=\"endEventRejected\">\n");
            xml.append("      <conditionExpression xsi:type=\"tFormalExpression\">${result == 'reject'}</conditionExpression>\n");
            xml.append("    </sequenceFlow>\n");
        }

        // 结束事件
        xml.append("    <endEvent id=\"endEventApproved\" name=\"审批通过\"/>\n");
        xml.append("    <endEvent id=\"endEventRejected\" name=\"审批驳回\"/>\n");

        // 闭合
        xml.append("  </process>\n");
        xml.append("</definitions>\n");

        return xml.toString();
    }
}
