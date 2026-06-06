export function createEmptyWorkflowDefinition() {
    return {
        nodes: [
            { id: 'start_1', type: 'START', name: 'Start', config: {} }
        ],
        edges: [],
        variables: []
    };
}
export function validateWorkflowDefinition(definition) {
    const issues = [];
    const nodeIds = new Set();
    const duplicateNodeIds = new Set();
    for (const node of definition.nodes) {
        if (nodeIds.has(node.id)) {
            duplicateNodeIds.add(node.id);
        }
        nodeIds.add(node.id);
    }
    for (const nodeId of duplicateNodeIds) {
        issues.push({
            code: 'DUPLICATE_NODE_ID',
            message: `节点 ID 重复：${nodeId}`,
            nodeId
        });
    }
    const startNodes = definition.nodes.filter((node) => node.type === 'START');
    if (startNodes.length !== 1) {
        issues.push({
            code: 'EXACTLY_ONE_START',
            message: '流程必须且只能有一个开始节点'
        });
    }
    if (!definition.nodes.some((node) => node.type === 'END')) {
        issues.push({
            code: 'AT_LEAST_ONE_END',
            message: '流程至少需要一个结束节点'
        });
    }
    const adjacency = new Map();
    for (const edge of definition.edges) {
        const sourceExists = nodeIds.has(edge.sourceNodeId);
        const targetExists = nodeIds.has(edge.targetNodeId);
        if (!sourceExists) {
            issues.push({
                code: 'EDGE_SOURCE_MISSING',
                message: `连线 ${edge.id} 的源节点不存在`,
                edgeId: edge.id
            });
        }
        if (!targetExists) {
            issues.push({
                code: 'EDGE_TARGET_MISSING',
                message: `连线 ${edge.id} 的目标节点不存在`,
                edgeId: edge.id
            });
        }
        if (sourceExists && targetExists) {
            adjacency.set(edge.sourceNodeId, [...(adjacency.get(edge.sourceNodeId) ?? []), edge.targetNodeId]);
        }
    }
    if (startNodes.length === 1) {
        const reachableNodeIds = resolveReachableNodeIds(startNodes[0].id, adjacency);
        for (const node of definition.nodes) {
            if (!reachableNodeIds.has(node.id)) {
                issues.push({
                    code: 'NODE_UNREACHABLE',
                    message: `节点「${node.name}」无法从开始节点到达`,
                    nodeId: node.id
                });
            }
        }
    }
    return issues;
}
function resolveReachableNodeIds(startNodeId, adjacency) {
    const reachableNodeIds = new Set();
    const queue = [startNodeId];
    while (queue.length) {
        const nodeId = queue.shift();
        if (reachableNodeIds.has(nodeId)) {
            continue;
        }
        reachableNodeIds.add(nodeId);
        queue.push(...(adjacency.get(nodeId) ?? []));
    }
    return reachableNodeIds;
}
//# sourceMappingURL=index.js.map