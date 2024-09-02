# partial equivalance数量变化

- 仅添加转发规则：71个

- 添加了ip封装解封装：79个

- 添加vni规则：553（=79*7，恰好是添加了6个vni后产生的七个正交空间）

- 添加vni封装：630（封装规则时候）

- 添加gbp：8190，扩大了13倍。理论上最多会变为原来的100倍（10个，epg 10个，各自分离）。

- 添加epg规则，epg规则，添加的内容，数量大幅增加，13942

  

- fib rules number is: 104
  after fibs: 71 time: 76
  after vxlan ip encap: 79 43
  after update rewrite table: 79 3
  after merge: 79 0
  after vxlan encap: 79 time: 50
  after vni identifier: 79 time: 0
  after vni encap*: 79 1
  after vni encap: 79 time: 1
  after vxlan ip encap: 79 time: 1
  after gbp: 1027 617





# 为什么添加了epg之后原子增加幅度如此之大



```
+ epg leaf-0-1_vpn0 167773185/24 24 40002
+ epg leaf-0-4_vpn0 167775745/24 24 40000
+ epg leaf-0-1_vpn3 167772929/24 24 40006
+ epg leaf-0-0_vpn5 167772161/24 24 40007
+ epg leaf-0-4_vpn5 167776001/24 24 40003
+ epg leaf-0-1_vpn5 167772161/24 24 40007
+ epg leaf-0-2_vpn0 167774721/24 24 40001
+ epg leaf-0-4_vpn1 167776257/24 24 40009
+ epg leaf-0-3_vpn1 167773697/24 24 40008
+ epg leaf-0-5_vpn3 167776513/24 24 40005
+ epg leaf-0-0_vpn0 167773185/24 24 40002
+ epg leaf-0-5_vpn0 167775745/24 24 40000
+ epg leaf-0-0_vpn3 167772929/24 24 40006
+ epg leaf-0-2_vpn5 167773953/24 24 40004
+ epg leaf-0-3_vpn5 167773953/24 24 40004
+ epg leaf-0-2_vpn1 167773697/24 24 40008
+ epg leaf-0-4_vpn3 167776513/24 24 40005
+ epg leaf-0-5_vpn1 167776257/24 24 40009
+ epg leaf-0-3_vpn0 167774721/24 24 40001
+ epg leaf-0-5_vpn5 167776001/24 24 40003

```



添加封装的规则是

封装element，如果封装都是，把ip封装成epgbdd





如果只是dstepg，划分的非常少，从1027到1145

但是涉及到了src epg，变化非常大

1027
19039



- 只添加epg

纯dst epg有10种

但是dst ^ src 有12种

解释了13是怎么来的





- 添加epg

- 添加epg的变化 
  - 只添加dstepg：如果只是dstepg，划分的非常少，从1027到1145
  - 只添加srcepg：变化的数量是2*N+1倍。原因：去掉rewrite之后的结果是N+1非常和期望相同，但是添加了rewrite后，变成了21倍，因为rewrite实际上变成了A-> AB。之前需要把A从原本的里面拆分出来，也就变成了用什么来表示A，把原本的都拆分一下，也就是N+1个的原因。然后是AB之后，需要想办法把AB这个表示出来，因为A已经可以被表示出来了，后续只需要





# 只添加了epg验证网络

vpn到encapsrc到underlay到encapdst，到decapdst，到vpn。总结下来需要涉及到的

涉及到的原子划分过程

纯ip+封装ip：79个原子

添加了gbr：*13

添加了epg：*21

添加了vni：*7

添加了vni封装：+80（ip的角度 553->630）（79->152）

17847：

涉及到的匹配域：源ip，目的ip（内外层一致），epg





如果放弃了updated

到1027为止都没变（ip+encap+gbr）

然后是



11297：1027=11

也就是十个src的内容。

[leaf-0-5_vpn0, leaf-0-5_vpn0_encapip, leaf-0-5, spine-0-1, leaf-0-1, leaf-0-1_decapip, leaf-0-1_vpn0, leaf-0-1_vpn0_encapip, leaf-0-1, spine-0-1, leaf-0-3, leaf-0-3_decapip, leaf-0-3_vpn0, leaf-0-3_bd4]

[leaf-0-5_vpn0, leaf-0-5_vpn0_encapip, leaf-0-5, spine-0-1, leaf-0-1, leaf-0-1_decapip, leaf-0-1_vpn0, leaf-0-1_vpn0_encapip, leaf-0-1, spine-0-1, leaf-0-3, leaf-0-3_decapip, leaf-0-3_vpn0, leaf-0-3_vpn0_encapip, leaf-0-3, spine-0-1, core-0-1, core-0-1_decapip, core-0-1_vpn0, core-0-1_bd0]

[leaf-0-5_vpn0, leaf-0-5_vpn0_encapip, leaf-0-5, spine-0-1, leaf-0-1, leaf-0-1_decapip, leaf-0-1_vpn0, leaf-0-1_vpn0_encapip, leaf-0-1, spine-0-1, leaf-0-3, leaf-0-3_decapip, leaf-0-3_vpn0, leaf-0-3_vpn0_encapip, leaf-0-3, spine-0-1, core-0-0, core-0-0_decapip, core-0-0_vpn0, core-0-0_bd0]

[leaf-0-5_vpn0, leaf-0-5_vpn0_encapip, leaf-0-5, spine-0-1, leaf-0-1, leaf-0-1_decapip, leaf-0-1_vpn0, leaf-0-1_vpn0_encapip, leaf-0-1, spine-0-1, leaf-0-2, leaf-0-2_decapip, leaf-0-2_vpn0, leaf-0-2_bd4]

、
