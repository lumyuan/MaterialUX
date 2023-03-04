# MaterialUX
一个基于androidx的质感UI、UX组件库

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
* 模块：Core(必须)、BottomNavigationView、TopBar，更多组件开发中...
* 版本：[![](https://jitpack.io/v/lumyuan/MaterialUX.svg)](https://jitpack.io/#lumyuan/MaterialUX)
```gradle
// Java 项目必须
implementation 'androidx.core:core-ktx:1.9.0'
implementation 'com.github.lumyuan.MaterialUX:TopBar:{version-name}' //组件库必须

//可选模块
implementation 'com.github.lumyuan.MaterialUX:BottomNavigationView:{version-name}'
implementation 'com.github.lumyuan.MaterialUX:Core:{version-name}'
```

## 代码：查阅<a href="https://github.com/lumyuan/MaterialUX/blob/main/app/src/main/java/io/github/lumyuan/ux/MainActivity.java">app/src/main/java/io/github/lumyuan/ux/MainActivity.java</a>

# License
```license
Material UX - the ui & ux component gallery for Android
https://github.com/lumyuan/MaterialUX
Copyright (C) 2020-2023  LumYuan

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
USA

Please contact LumYuan by email 2205903933@qq.com if you need
additional information or have any questions
```
