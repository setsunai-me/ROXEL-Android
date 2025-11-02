## Download
Download [`AAR`](https://github.com/setsunai-me/ROXEL-Android/releases/tag/release) library file, move it to `app/libs` 

Add to dependencies `build.gradle.kts` (app)
```Kotlin
implementation(files("libs/roxel-<version>.aar"))
```

Change SDK support parameters
```gradle
minSdk = 28
targetSdk = 35
```

Add dependencies data to `gradle/libs.versions.toml`
```toml
[versions]
lifecycleKtx = "2.9.4"
gson = "2.13.2"

[libraries]
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycleKtx" }
androidx-lifecycle-viewmodel-ktx = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycleKtx" }
gson = { module = "com.google.code.gson:gson", version.ref = "gson" }
```

Add to dependencies `build.gradle.kts` (app)
```gradle
implementation(libs.androidx.lifecycle.viewmodel.ktx)
implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.gson)
```

## Quick start

Required permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
```

Create a ROXEL instance in the main activity
```kotlin
private val sdk: Roxel = Roxel.Builder(this)
        .ssid("<YOUR_WIFI_SSID>")      // for rooted devices only
        .passkey("YOUR_WIFI_PASSKEY")  // for rooted devices only
        .hidden(false)                 // for rooted devices only
        .build()
```

Create a ROXEL connection instance
```kotlin
private val instance: Roxel.Instance = Roxel.Instance.Builder(sdk, "main")
        .ip("<ROXEL_SERVER_IP>")
        .token("<ROXEL_SERVER_TOKEN>")
        .udp(<ROXEL_SERVER_UDP_PORT>)
        .tcp(<ROXEL_SERVER_TCP_PORT>)
        .build()
```

Use the base function to monitor the connection state
```kotlin
sdk.listen { state ->
}
```

Use the base controller to create hooks
```kotlin
controller(instance) {
        // here your hooks
}
```

To create hooks inside a controller, use special methods
```kotlin
inline fun <reified T : Serializable> useRequest(id: String)
inline fun <reified T> useStatesEffect(id: String, size: Int = 32)
inline fun <reified T> useEffect(id: String)
```

Example of using hooks:
```kotlin
controller(instance) {
        val messagesHook: Request<Messages> by useRequest("messages")
        val indicatorsHook: StatesEffect<Boolean> by useStatesEffect("indicators")
        val lightHook: Effect<Int> by useEffect("light")
        val switchHook: Effect<Boolean> by useEffect("switch")
}
```

## Example of working with `Request`
> [!WARNING]
> `Request` only support `Serializable` types
```kotlin
controller(instance) {
        val messagesHook: Request<Messages> by useRequest("messages")

        // RECEIVE
        messagesHook.listen { message ->
        }

        // SEND
        messagesHook.send(MyEntity("ok"))
}
```

## Example of working with `Effect`
> [!WARNING]
> `Effect` only support basic primitive types such as `Int`, `Boolean` 
```kotlin
controller(instance) {
        val switchHook: Effect<Boolean> by useEffect("switch")
        val lightHook: Effect<Int> by useEffect("light")

        // RECEIVE
        switchHook.listen { state ->
        }
        lightHook.listen { value ->
        }

        // UPDATE
        switchHook.set(true);
        lightHook.set(35);
}
```

## Example of working with `StatesEffect`
> [!NOTE]
> `StatesEffect` works on the same principle as `Effect`, except that it is based on a `32-bit value`, where it stores the state in each bit and has minor changes in functions.

> [!WARNING]
> `StatesEffect` only support basic primitive types such as `Int`, `Boolean`

> [!WARNING]
> `StatesEffect` has a limit of strictly `32` states.
```kotlin
controller(instance) {
        val statesHook: StateEffect<Boolean> by useStatesEffect("states", 32)

        // RECEIVE
        statesHook.listen(5) { state ->
                // to listen at the specified index
        }
        statesHook.listenAll { allStates ->
                // to listen for the overall state (1 or true - when all states are 1 or true, otherwise - 0 or false)
        }

        // UPDATE
        statesHook.set(3, true) // to set the value at the specified index
        statesHook.set(false) // to set all values
}
```

## Launch
```kotlin
sdk.launch(instance)
```
or
```kotlin
sdk.launch("main") // Use the ID that was specified in the desired instance.
```
