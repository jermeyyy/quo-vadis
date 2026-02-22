# NodeKey Refactor - Remaining Changes Needed

## COMPLETED so far:
1. NodeKey.kt - created
2. NavNode.kt - key/parentKey changed to NodeKey
3. ScreenNode.kt, StackNode.kt, TabNode.kt, PaneNode.kt - updated
4. TypeAliases.kt - NavKeyGenerator returns NodeKey
5. KeyGenerator.kt - returns NodeKey
6. TreeMutator.kt - all String key params → NodeKey (JUST DONE)
7. PushOperations.kt - stackKey → NodeKey, keyGenerator default wraps in NodeKey (JUST DONE)
8. TabOperations.kt - tabNodeKey → NodeKey (JUST DONE)
9. PaneOperations.kt - all nodeKey/paneNodeKey → NodeKey (JUST DONE)

## STILL TODO:

### TreeNodeOperations.kt
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`
- `replaceNode(root, targetKey: String, ...)` → `targetKey: NodeKey`
- `tryReplaceNode(root, targetKey: String, ...)` → `targetKey: NodeKey`
- `removeNode(root, targetKey: String)` → `targetKey: NodeKey`
- In comparisons: `root.key == targetKey` works since both NodeKey
- In error messages: `'$targetKey'` works because NodeKey.toString() returns value

### PopOperations.kt
- No explicit key params needed to change BUT `popTo(root, targetKey: String, ...)` needs NodeKey
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`
- `popTo(root, targetKey: String, inclusive)` → `targetKey: NodeKey` at line ~119-ish (the non-inline overload)
- In the inline popTo, the predicate uses `node.key` which is NodeKey - that's fine

### TreeDiffCalculator.kt
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`
- `TreeDiff.removedScreenKeys: Set<String>` → `Set<NodeKey>`
- `NodeInfo.lifecycleNodeKeys: Set<String>` → `Set<NodeKey>`
- `NodeInfo.screenKeys: Set<String>` → `Set<NodeKey>`
- `LifecycleNodeEntry.key: String` → `key: NodeKey`
- Internal mutable sets: `mutableSetOf<String>()` → works since node.key is NodeKey

### LifecycleNotifier.kt
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`
- `collectLifecycleAwareNodeKeys` returns `Set<String>` → `Set<NodeKey>`
- Internal `keys: MutableSet<String>` → `MutableSet<NodeKey>` (node.key is NodeKey so add() works)

### ScreenKeyCollector.kt
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`
- `cancelResultsForKeys(removedScreenKeys: Set<String>)` → `Set<NodeKey>`
- Inside: `resultManager.cancelResult(screenKey)` → `resultManager.cancelResult(screenKey.value)` (boundary)
- `collectScreenKeys` returns `Set<String>` → `Set<NodeKey>`
- Internal `keys: MutableSet<String>` → `MutableSet<NodeKey>`

### CascadeBackState.kt
- Add `import com.jermey.quo.vadis.core.navigation.node.NodeKey`
- `val animatingStackKey: String?` → `val animatingStackKey: NodeKey?`
- This fixes errors in CascadeBackState (4 places), PaneRenderer (line 277), NavRenderScope (line 245)

### TreeNavigator.kt
- `private fun generateKey(): String = Uuid.random().toString().take(8)` → `private fun generateKey(): NodeKey = NodeKey(Uuid.random().toString().take(8))`
- `fromKey: String?` in navigateWithContainer/navigateDefault → `fromKey: NodeKey?`
- At TransitionManager boundary (takes String?):
  - `transitionManager.startNavigationTransition(effectiveTransition, fromKey, toKey)` → `transitionManager.startNavigationTransition(effectiveTransition, fromKey?.value, toKey?.value)`
  - Same for all other calls
- `containerInfo.builder(containerKey, rootKey, ...)` - builder params were String, need NodeKey
  → Need to update ContainerRegistry.TabContainer/PaneContainer builder signatures first

### ContainerRegistry.kt (quo-vadis-core/.../registry/ContainerRegistry.kt)
- `TabContainer.builder: (key: String, parentKey: String?, initialTabIndex: Int) -> TabNode` → `(key: NodeKey, parentKey: NodeKey?, initialTabIndex: Int) -> TabNode` 
- `PaneContainer.builder: (key: String, parentKey: String?) -> PaneNode` → `(key: NodeKey, parentKey: NodeKey?) -> PaneNode`
- `TabsContainer(tabNodeKey: String, ...)` and `PaneContainer(paneNodeKey: String, ...)` composable functions - KEEP as String (these are registry lookup keys, not node keys)
- Add import for NodeKey

### NavigationHost.kt
- `ScreenNavigationInfo(screenId = it.key, ...)` → `screenId = it.key.value` (3 occurrences)

### PaneRenderer.kt
- `val cacheKey = node.key` → `val cacheKey = node.key.value` (for CachedEntry which takes String)
- `paneNodeKey = node.scopeKey ?: node.key` → `paneNodeKey = node.scopeKey ?: node.key.value` (for PaneContainer which takes String)
- Line 277 cascadeState comparison → fixed by changing CascadeBackState.animatingStackKey to NodeKey?

### ScreenRenderer.kt
- `key = node.key` → `key = node.key.value` (for CachedEntry which takes String)

### TabRenderer.kt
- `key = node.key` → `key = node.key.value` (for CachedEntry which takes String)
- `tabNodeKey = node.wrapperKey ?: node.key` → `tabNodeKey = node.wrapperKey ?: node.key.value` (for TabsContainer which takes String)

### NavRenderScope.kt
- Line 245 comparison: `node.key == animatingKey` where animatingKey from cascadeState.animatingStackKey
  → Fixed when CascadeBackState.animatingStackKey becomes NodeKey?

### DslNavigationConfig.kt (both dsl/ and dsl/internal/)
- Keep `buildNavNode(key: String?, parentKey: String?)` as-is (interface boundary)
- In private build methods like `buildStackNode(builder, key: String, parentKey: String?)`:
  - Wrap with NodeKey: `StackNode(key = NodeKey(key), parentKey = parentKey?.let { NodeKey(it) }, ...)`
  - Same pattern for all node constructors

### CompositeNavigationConfig.kt (both copies at config/ and internal/config/)
- Keep `buildNavNode(key: String?, parentKey: String?)` as-is (matches interface)

### EmptyNavigationConfig.kt
- Keep `buildNavNode(key: String?, parentKey: String?)` as-is (matches interface)

### BackOperations.kt
- Check for `keys.add(node.key)` patterns - `node.key` is NodeKey, but there's no explicit String key usage
- Likely NO changes needed (types flow through NavNode operations)
- May need import if NodeKey appears explicitly

### PopOperations.kt in `popTo(root, targetKey, ...)` non-inline overload:
- `node.key == targetKey` - if targetKey is NodeKey, this comparison works

### Test file: CascadeBackStateTest.kt
- May need updates for `animatingStackKey` type change
