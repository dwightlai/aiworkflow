const NODE_WIDTH = 180;
const NODE_HEIGHT = 76;
const COLUMN_GAP = 230;
const ROW_GAP = 118;
const PADDING = 32;
const PORT_OFFSET = 7;
const DUPLICATE_OFFSET = 40;
export function createWorkflowDesignerLayout(definition) {
    return createLayout(definition, true);
}
function createLayout(definition, useStoredPositions) {
    const levels = resolveNodeLevels(definition);
    const groupedNodes = definition.nodes.reduce((groups, node) => {
        const level = levels.get(node.id) ?? 0;
        groups[level] = [...(groups[level] ?? []), node];
        return groups;
    }, {});
    const layoutNodes = Object.entries(groupedNodes).flatMap(([levelValue, nodes]) => {
        const level = Number(levelValue);
        return nodes.map((node, index) => {
            const position = readNodePosition(node);
            return {
                ...node,
                x: useStoredPositions ? position?.x ?? PADDING + level * COLUMN_GAP : PADDING + level * COLUMN_GAP,
                y: useStoredPositions ? position?.y ?? PADDING + index * ROW_GAP : PADDING + index * ROW_GAP,
                width: NODE_WIDTH,
                height: NODE_HEIGHT
            };
        });
    });
    const nodeById = new Map(layoutNodes.map((node) => [node.id, node]));
    const layoutEdges = definition.edges.flatMap((edge) => {
        const source = nodeById.get(edge.sourceNodeId);
        const target = nodeById.get(edge.targetNodeId);
        if (!source || !target) {
            return [];
        }
        const sourceX = source.x + source.width + PORT_OFFSET;
        const sourceY = source.y + source.height / 2;
        const targetX = target.x - PORT_OFFSET;
        const targetY = target.y + target.height / 2;
        const curveOffset = Math.max((targetX - sourceX) / 2, 60);
        return [{
                ...edge,
                sourceX,
                sourceY,
                targetX,
                targetY,
                path: `M ${sourceX} ${sourceY} C ${sourceX + curveOffset} ${sourceY}, ${targetX - curveOffset} ${targetY}, ${targetX} ${targetY}`
            }];
    });
    const maxX = layoutNodes.reduce((value, node) => Math.max(value, node.x + node.width), PADDING);
    const maxY = layoutNodes.reduce((value, node) => Math.max(value, node.y + node.height), PADDING);
    return {
        nodes: layoutNodes,
        edges: layoutEdges,
        bounds: {
            width: maxX + PADDING,
            height: maxY + PADDING
        }
    };
}
export function createWorkflowDesignerCore(options) {
    let currentValue = options.value;
    let selectedNodeId = null;
    function emit(nextValue) {
        currentValue = nextValue;
        options.onChange?.(nextValue);
    }
    function updateValue(updater) {
        if (options.readonly) {
            return;
        }
        emit(updater(currentValue));
    }
    return {
        mount() {
            options.container.dataset.workflowDesignerMounted = 'true';
        },
        destroy() {
            delete options.container.dataset.workflowDesignerMounted;
        },
        getValue() {
            return currentValue;
        },
        setValue(value) {
            const nodeStillExists = !selectedNodeId || value.nodes.some((node) => node.id === selectedNodeId);
            if (!nodeStillExists) {
                selectedNodeId = null;
            }
            emit(value);
        },
        addNode(node) {
            updateValue((value) => ({
                ...value,
                nodes: [...value.nodes.filter((item) => item.id !== node.id), node]
            }));
        },
        duplicateNode(nodeId) {
            const sourceNode = currentValue.nodes.find((node) => node.id === nodeId);
            if (!sourceNode || options.readonly) {
                return null;
            }
            const copyIndex = nextCopyIndex(currentValue.nodes, nodeId);
            const position = readNodePosition(sourceNode);
            const copy = {
                ...sourceNode,
                id: `${nodeId}_copy_${copyIndex}`,
                name: `${sourceNode.name} 副本`,
                config: {
                    ...cloneConfig(sourceNode.config),
                    ui: {
                        ...(isRecord(sourceNode.config.ui) ? sourceNode.config.ui : {}),
                        position: {
                            x: (position?.x ?? PADDING) + DUPLICATE_OFFSET,
                            y: (position?.y ?? PADDING) + DUPLICATE_OFFSET
                        }
                    }
                }
            };
            updateValue((value) => ({
                ...value,
                nodes: [...value.nodes, copy]
            }));
            return copy;
        },
        updateNode(nodeId, patch) {
            updateValue((value) => ({
                ...value,
                nodes: value.nodes.map((node) => node.id === nodeId ? { ...node, ...patch, id: node.id } : node)
            }));
        },
        updateEdge(edgeId, patch) {
            updateValue((value) => ({
                ...value,
                edges: value.edges.map((edge) => edge.id === edgeId ? { ...edge, ...patch, id: edge.id } : edge)
            }));
        },
        moveNode(nodeId, x, y) {
            updateValue((value) => ({
                ...value,
                nodes: value.nodes.map((node) => node.id === nodeId ? {
                    ...node,
                    config: {
                        ...node.config,
                        ui: {
                            ...(isRecord(node.config.ui) ? node.config.ui : {}),
                            position: {
                                x: Math.max(Math.round(x), 0),
                                y: Math.max(Math.round(y), 0)
                            }
                        }
                    }
                } : node)
            }));
        },
        removeNode(nodeId) {
            updateValue((value) => ({
                ...value,
                nodes: value.nodes.filter((node) => node.id !== nodeId),
                edges: value.edges.filter((edge) => edge.sourceNodeId !== nodeId && edge.targetNodeId !== nodeId)
            }));
            if (selectedNodeId === nodeId) {
                selectedNodeId = null;
            }
        },
        removeEdge(edgeId) {
            updateValue((value) => ({
                ...value,
                edges: value.edges.filter((edge) => edge.id !== edgeId)
            }));
        },
        connectNodes(edgeId, sourceNodeId, targetNodeId) {
            const edge = {
                id: edgeId,
                sourceNodeId,
                targetNodeId,
                condition: null
            };
            updateValue((value) => ({
                ...value,
                edges: [...value.edges.filter((item) => item.id !== edgeId), edge]
            }));
        },
        autoLayout() {
            updateValue((value) => {
                const layout = createLayout(value, false);
                const positionByNodeId = new Map(layout.nodes.map((node) => [node.id, { x: node.x, y: node.y }]));
                return {
                    ...value,
                    nodes: value.nodes.map((node) => ({
                        ...node,
                        config: {
                            ...node.config,
                            ui: {
                                ...(isRecord(node.config.ui) ? node.config.ui : {}),
                                position: positionByNodeId.get(node.id) ?? { x: PADDING, y: PADDING }
                            }
                        }
                    }))
                };
            });
        },
        selectNode(nodeId) {
            selectedNodeId = nodeId && currentValue.nodes.some((node) => node.id === nodeId) ? nodeId : null;
        },
        getSelectedNode() {
            return currentValue.nodes.find((node) => node.id === selectedNodeId) ?? null;
        }
    };
}
function resolveNodeLevels(definition) {
    const incomingCount = new Map(definition.nodes.map((node) => [node.id, 0]));
    const outgoingEdges = new Map();
    for (const edge of definition.edges) {
        incomingCount.set(edge.targetNodeId, (incomingCount.get(edge.targetNodeId) ?? 0) + 1);
        outgoingEdges.set(edge.sourceNodeId, [...(outgoingEdges.get(edge.sourceNodeId) ?? []), edge]);
    }
    const queue = definition.nodes
        .filter((node) => node.type === 'START' || (incomingCount.get(node.id) ?? 0) === 0)
        .map((node) => node.id);
    const levels = new Map();
    for (const nodeId of queue) {
        levels.set(nodeId, 0);
    }
    while (queue.length) {
        const nodeId = queue.shift();
        const currentLevel = levels.get(nodeId) ?? 0;
        for (const edge of outgoingEdges.get(nodeId) ?? []) {
            const nextLevel = Math.max(levels.get(edge.targetNodeId) ?? 0, currentLevel + 1);
            levels.set(edge.targetNodeId, nextLevel);
            incomingCount.set(edge.targetNodeId, (incomingCount.get(edge.targetNodeId) ?? 1) - 1);
            if ((incomingCount.get(edge.targetNodeId) ?? 0) <= 0) {
                queue.push(edge.targetNodeId);
            }
        }
    }
    definition.nodes.forEach((node, index) => {
        if (!levels.has(node.id)) {
            levels.set(node.id, index);
        }
    });
    return levels;
}
function readNodePosition(node) {
    const ui = node.config.ui;
    if (!isRecord(ui) || !isRecord(ui.position)) {
        return null;
    }
    const { x, y } = ui.position;
    return typeof x === 'number' && typeof y === 'number' ? { x, y } : null;
}
function isRecord(value) {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}
function cloneConfig(config) {
    return JSON.parse(JSON.stringify(config));
}
function nextCopyIndex(nodes, nodeId) {
    let index = 1;
    const ids = new Set(nodes.map((node) => node.id));
    while (ids.has(`${nodeId}_copy_${index}`)) {
        index += 1;
    }
    return index;
}
//# sourceMappingURL=index.js.map