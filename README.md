# megacubo-pip

PIP plugin for Megacubo project.

## Install

```bash
npm install megacubo-pip
npx cap sync
```

## API

<docgen-index>

* [`enter(...)`](#enter)
* [`isPip()`](#ispip)
* [`autoPIP(...)`](#autopip)
* [`aspectRatio(...)`](#aspectratio)
* [`onPipModeChanged()`](#onpipmodechanged)
* [`isPipModeSupported()`](#ispipmodesupported)
* [`addListener('onPipModeChanged', ...)`](#addlisteneronpipmodechanged-)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### enter(...)

```typescript
enter(options?: { width?: number | undefined; height?: number | undefined; } | undefined) => Promise<{ value: string; }>
```

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code>{ width?: number; height?: number; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### isPip()

```typescript
isPip() => Promise<{ value: boolean; }>
```

**Returns:** <code>Promise&lt;{ value: boolean; }&gt;</code>

--------------------


### autoPIP(...)

```typescript
autoPIP(options: { value: boolean; width?: number; height?: number; }) => Promise<{ value: string; }>
```

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code>{ value: boolean; width?: number; height?: number; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### aspectRatio(...)

```typescript
aspectRatio(options: { width: number; height: number; }) => Promise<{ value: string; }>
```

| Param         | Type                                            |
| ------------- | ----------------------------------------------- |
| **`options`** | <code>{ width: number; height: number; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### onPipModeChanged()

```typescript
onPipModeChanged() => Promise<void>
```

--------------------


### isPipModeSupported()

```typescript
isPipModeSupported() => Promise<{ value: boolean; }>
```

**Returns:** <code>Promise&lt;{ value: boolean; }&gt;</code>

--------------------


### addListener('onPipModeChanged', ...)

```typescript
addListener(eventName: 'onPipModeChanged', callback: (data: { value: boolean; }) => void) => Promise<void>
```

| Param           | Type                                                |
| --------------- | --------------------------------------------------- |
| **`eventName`** | <code>'onPipModeChanged'</code>                     |
| **`callback`**  | <code>(data: { value: boolean; }) =&gt; void</code> |

--------------------

</docgen-api>
