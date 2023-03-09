# MaterialUX
一个基于androidx的质感UI、UX组件库（基于Kotlin），<a href="https://github.com/lumyuan/MaterialUX/releases">下载预览APK</a>

# 使用
1. 在你的项目下的build.gradle文件或项目下的settings.gradle文件中：
 ```gradle
 allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
 ```
2. 导入依赖
* 模块：Core(必须)、BottomNavigationView、TopBar、GroundGlassView，更多组件开发中...
* 版本：[![](https://jitpack.io/v/lumyuan/MaterialUX.svg)](https://jitpack.io/#lumyuan/MaterialUX)
```gradle
// Java 项目必须
implementation 'androidx.core:core-ktx:1.9.0'
implementation 'com.github.lumyuan.MaterialUX:Core:{version-name}' //组件库必须

//可选模块
implementation 'com.github.lumyuan.MaterialUX:BottomNavigationView:{version-name}'
implementation 'com.github.lumyuan.MaterialUX:TopBar:{version-name}'
implementation 'com.github.lumyuan.MaterialUX:GroundGlassView:{version-name}'
implementation 'com.github.lumyuan.MaterialUX:CircleSeekBar:{version-name}'
```

## 代码：查阅<a href="https://github.com/lumyuan/MaterialUX/blob/main/app/src/main/java/io/github/lumyuan/ux/MainActivity.java">app/src/main/java/io/github/lumyuan/ux/MainActivity.java</a>

# License
```
https://github.com/lumyuan/MaterialUX
Copyright 2023 lumyuan

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

Please contact LumYuan by email 2205903933@qq.com if you need
additional information or have any questions
```
