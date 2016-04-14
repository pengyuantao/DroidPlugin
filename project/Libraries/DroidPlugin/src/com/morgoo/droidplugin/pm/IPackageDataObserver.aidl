/*
**        DroidPlugin Project
**
** Copyright(c) 2015 Andy Zhang <zhangyong232@gmail.com>
**
** This file is part of DroidPlugin.
**
** DroidPlugin is free software: you can redistribute it and/or
** modify it under the terms of the GNU Lesser General Public
** License as published by the Free Software Foundation, either
** version 3 of the License, or (at your option) any later version.
**
** DroidPlugin is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
** Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public
** License along with DroidPlugin.  If not, see <http://www.gnu.org/licenses/lgpl.txt>
**
**/

package com.morgoo.droidplugin.pm;

/**
 * API for package data change related callbacks from the Package Manager.
 * Some usage scenarios include deletion of cache directory, generate
 * statistics related to code, data, cache usage
 *
 * 其中AIDL的方法还提供了oneway这个关键字，
 * 可以用关键字oneway来标明远程调用的行为属性，
 * 使用了该关键字，
 * 那么远程调用将仅仅是调用所需的数据传输过来并立即返回，
 * 而不会等待结果的返回，
 * 也即是说不会阻塞远程线程的运行。
 * AIDL接口将最终将获得一个从Binder线程池中产生的调用（和普通的远程调用类似）。
 * 如果关键字oneway在本地调用中被使用，将不会对函数调用有任何影响。
 */
oneway interface IPackageDataObserver {
    void onRemoveCompleted(in String packageName, boolean succeeded);
}
