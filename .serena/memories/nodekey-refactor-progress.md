# NodeKey Refactor Progress (KOTLIN-1) - Updated

## Status: IN PROGRESS - Core Types Done

## Completed Changes
1. **NodeKey.kt** - Created at `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/node/NodeKey.kt`
2. **NavNode.kt** - Changed `key: String` → `key: NodeKey`, `parentKey: String?` → `parentKey: NodeKey?`, `findByKey(key: String)` → `findByKey(key: NodeKey)`
3. **ScreenNode.kt** - Changed constructor `key: String` → `key: NodeKey`, `parentKey: String?` → `parentKey: NodeKey?`
4. **StackNode.kt** - Changed constructor `key: String` → `key: NodeKey`, `parentKey: String?` → `parentKey: NodeKey?`
5. **TabNode.kt** - Changed constructor + copy function key/parentKey types to NodeKey
6. **PaneNode.kt** - Changed constructor + copy function key/parentKey types to NodeKey
7. **TypeAliases.kt** - Changed `NavKeyGenerator = () -> String` → `() -> NodeKey`, added NodeKey import
8. **KeyGenerator.kt** - Changed `generate(): String` → `generate(): NodeKey`, wraps UUID in NodeKey

## Remaining Changes Needed

### TreeMutator.kt (quo-vadis-core/.../internal/tree/TreeMutator.kt)
Change ALL String key parameters to NodeKey:
- `pushToStack(root, stackKey: String, ...)` → `stackKey: NodeKey`
- `clearStackAndPush(root, stackKey: String, ...)` → `stackKey: NodeKey`
- `switchTab(root, tabNodeKey: String, ...)` → `tabNodeKey: NodeKey`
- `navigateToPane(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- `switchActivePane(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- `popPane(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- `setPaneConfiguration(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- `removePaneConfiguration(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- `replaceNode(root, targetKey: String, ...)` → `targetKey: NodeKey`
- `removeNode(root, targetKey: String, ...)` → `targetKey: NodeKey`
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

### PushOperations.kt
- `keyGenerator` field: `NavKeyGenerator = { Uuid.random().toString().take(8) }` → `{ NodeKey(Uuid.random().toString().take(8)) }`
- `pushToStack(root, stackKey: String, ...)` → `stackKey: NodeKey`
- `clearStackAndPush(root, stackKey: String, ...)` → `stackKey: NodeKey`
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

### PopOperations.kt
- `popTo(root, targetKey: String, ...)` method (non-inline) → `targetKey: NodeKey`
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

### TabOperations.kt
- `switchTab(root, tabNodeKey: String, ...)` → `tabNodeKey: NodeKey`
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

### PaneOperations.kt
- ALL `nodeKey: String` params → `nodeKey: NodeKey`
- `clearPaneStack(root, paneNodeKey: String, ...)` → `paneNodeKey: NodeKey`
- `navigateToPane(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- `switchActivePane(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- `popPane(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- `setPaneConfiguration(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- `removePaneConfiguration(root, nodeKey: String, ...)` → `nodeKey: NodeKey`
- Default keyGenerator in navigateToPane: `{ Uuid.random().toString().take(8) }` → `{ NodeKey(Uuid.random().toString().take(8)) }`
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

### TreeNodeOperations.kt
- `replaceNode(root, targetKey: String, ...)` → `targetKey: NodeKey`
- `tryReplaceNode(root, targetKey: String, ...)` → `targetKey: NodeKey`
- `removeNode(root, targetKey: String, ...)` → `targetKey: NodeKey`
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

### BackOperations.kt
- No explicit String key params in public API
- Uses `parentKey` (now NodeKey?) with findByKey(NodeKey) — should work via smart cast after null checks
- `grandparentKey` local vars get smart-cast from NodeKey? to NodeKey after null checks
- Likely NO changes needed (types flow through)
- BUT need to add `import com.jermey.quo.vadis.core.navigation.node.NodeKey` if NodeKey is used anywhere

### TreeDiffCalculator.kt
- `TreeDiff.removedScreenKeys: Set<String>` → `Set<NodeKey>`
- `NodeInfo.lifecycleNodeKeys: Set<String>` → `Set<NodeKey>`
- `NodeInfo.screenKeys: Set<String>` → `Set<NodeKey>`
- `LifecycleNodeEntry.key: String` → `NodeKey`
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

### LifecycleNotifier.kt
- `collectLifecycleAwareNodeKeys` returns `Set<String>` → `Set<NodeKey>`  
- Internal `keys: MutableSet<String>` → `MutableSet<NodeKey>`
- `node.key` is already NodeKey so `keys.add(node.key)` will just work
- `node.key !in newNodeKeys` — compares String to Set<String> currently. After change: NodeKey to Set<NodeKey>. Works.
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

### ScreenKeyCollector.kt
- `cancelResultsForKeys(removedScreenKeys: Set<String>)` → `Set<NodeKey>`
- Inside: `resultManager.cancelResult(screenKey)` → `resultManager.cancelResult(screenKey.value)` (boundary conversion)
- `collectScreenKeys` returns `Set<String>` → `Set<NodeKey>`
- Internal `keys: MutableSet<String>` → `MutableSet<NodeKey>`
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

### TreeNavigator.kt
- `generateKey(): String` → `generateKey(): NodeKey` — wrap: `NodeKey(Uuid.random().toString().take(8))`
- `fromKey: String?` param in `navigateWithContainer` and `navigateDefault` → `NodeKey?`
- At TransitionManager boundary calls:
  - `transitionManager.startNavigationTransition(effectiveTransition, fromKey, toKey)` 
    → `transitionManager.startNavigationTransition(effectiveTransition, fromKey?.value, toKey?.value)`
  - `transitionManager.startNavigationTransition(effectiveTransition, fromKey, screenKey)`
    → `transitionManager.startNavigationTransition(effectiveTransition, fromKey?.value, screenKey.value)`
  - `transitionManager.startNavigationTransition(effectiveTransition, fromKey, newState.activeLeaf()?.key)`
    → `...fromKey?.value, newState.activeLeaf()?.key?.value)`
- In `navigateWithContainer` catch block: `generateKey()` calls now return NodeKey, used with constructors — works
- In `navigateDefault` catch block: same
- In `navigateToPane`: `ScreenNode(key = generateKey(), ...)` — works since key is now NodeKey
- In `createRootStack`: `generateKey()` returns NodeKey — used as StackNode.key — works; used as child.copy(parentKey = rootKey) — works
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`
- In `pushContainer`: `generateKey()` returns NodeKey, used with `containerInfo.builder(containerKey, targetStack.key, ...)` — builder params need to be NodeKey too

### ContainerRegistry.kt (quo-vadis-core/.../registry/ContainerRegistry.kt)
- `TabContainer.builder: (key: String, parentKey: String?, initialTabIndex: Int) -> TabNode` → `(key: NodeKey, parentKey: NodeKey?, initialTabIndex: Int) -> TabNode`
- `PaneContainer.builder: (key: String, parentKey: String?) -> PaneNode` → `(key: NodeKey, parentKey: NodeKey?) -> PaneNode`
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`

## Boundary Conversions (leave as String)
- TransitionManager.startNavigationTransition(transition, fromKey: String?, toKey: String?) — stays String
- NavigationResultManager.cancelResult(screenKey: String) — stays String
- TransitionState.InProgress.fromKey/toKey: String? — stays String
- CascadeBackState.animatingStackKey: String? — stays String

## Verification Commands
```bash
./gradlew :quo-vadis-core:compileKotlinMetadata --no-daemon 2>&1 | tail -20
./gradlew :quo-vadis-core:compileKotlinDesktop --no-daemon 2>&1 | tail -20
```
