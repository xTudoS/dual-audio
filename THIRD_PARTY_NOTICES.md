# Third-party notices

Dual Audio is licensed under the GNU General Public License v3.0 or later (see `LICENSE`).
The APK bundles the following third-party libraries, under their own licenses. All of them
are compatible with GPL-3.0.

## MIT License

**Shizuku API**: `dev.rikka.shizuku:api`, `provider`, `aidl`, `shared`
<https://github.com/RikkaApps/Shizuku-API>

```
MIT License

Copyright (c) 2021 RikkaW

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Apache License 2.0

Full text: <https://www.apache.org/licenses/LICENSE-2.0.txt>

- **AndroidX** (appcompat, core, activity, fragment, lifecycle and transitive modules),
  Copyright The Android Open Source Project
- **Material Components for Android** (`com.google.android.material:material`), Copyright Google LLC
- **Kotlin standard library**, **kotlinx.coroutines**, **kotlinx.serialization**,
  **JetBrains annotations**, Copyright JetBrains s.r.o. and Kotlin Programming Language contributors
- **Guava ListenableFuture** (`com.google.guava:listenablefuture`), Copyright The Guava Authors
- **Error Prone annotations** (`com.google.errorprone:error_prone_annotations`), Copyright Google LLC
- **JSpecify** (`org.jspecify:jspecify`), Copyright The JSpecify Authors

To regenerate the dependency list:

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```
