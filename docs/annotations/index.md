---
layout: default
title: Annotations
nav_order: 4
has_children: true
---

# Annotations

Memoize uses annotations to mark methods for caching. The annotation is processed at compile time by AspectJ, which weaves caching logic around the annotated method.

## Available Annotations

| Annotation | Status | Description |
|---|---|---|
| [`@MemoizeThis`]({% link annotations/memoize-this.md %}) | Available | Cache method return values with configurable TTL, size, key converter, and eligibility criteria |

## How It Works

When you annotate a method with `@MemoizeThis`, the AspectJ compiler weaves an `@Around` advice that:

1. Checks if `Memoize` has been started (if not, the method executes normally)
2. Resolves a cache key from the method arguments
3. Looks up the key in the cache provider
4. On **cache hit**: returns the cached value without executing the method body
5. On **cache miss**: executes the method, checks eligibility, and stores the result

```java
import space.hypercode.core.annotations.MemoizeThis;

public class ProductService {

    @MemoizeThis(ttlInMs = 30000, size = 200)
    public Product findProduct(ProductId id) {
        return database.queryProduct(id);
    }
}
```

## Key Resolution

For caching to work, Memoize must be able to generate a cache key from the method arguments. The resolution order is:

1. **`useConfig=true`** -- key converter comes from a registered `MemoizationConfig`
2. **Explicit `converter`** -- a `MemoizationKeyConverter` class specified in the annotation
3. **`Memoizable` interface** -- if the single argument implements `Memoizable`, its `memoizationKey()` is used
4. **No converter found** -- caching is skipped; the method executes normally every time

See the [`@MemoizeThis` reference]({% link annotations/memoize-this.md %}) for details on each attribute.
