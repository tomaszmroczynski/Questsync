import json, struct, math, random
random.seed(11)

def build_glb(path, prims):
    buf=bytearray(); bvs=[]; acc=[]; mats=[]; pr=[]
    def align():
        while len(buf)%4: buf.append(0)
    for pi,p in enumerate(prims):
        verts=p["verts"]; tris=p["tris"]
        idx=bytearray()
        for (a,b,c) in tris: idx+=struct.pack("<III",a,b,c)
        align(); io=len(buf); buf+=idx
        align(); po=len(buf)
        mn=[1e9]*3; mx=[-1e9]*3; pos=bytearray()
        for (x,y,z) in verts:
            pos+=struct.pack("<fff",x,y,z)
            for k,c in enumerate((x,y,z)): mn[k]=min(mn[k],c); mx[k]=max(mx[k],c)
        buf+=pos
        bi=len(bvs); bvs.append({"buffer":0,"byteOffset":io,"byteLength":len(idx),"target":34963})
        bp=len(bvs); bvs.append({"buffer":0,"byteOffset":po,"byteLength":len(pos),"target":34964})
        ai=len(acc); acc.append({"bufferView":bi,"componentType":5125,"count":len(tris)*3,"type":"SCALAR"})
        ap=len(acc); acc.append({"bufferView":bp,"componentType":5126,"count":len(verts),"type":"VEC3","min":mn,"max":mx})
        col=p["mat"]["color"]
        md={"pbrMetallicRoughness":{"baseColorFactor":col,"metallicFactor":0.0,"roughnessFactor":1.0},
            "emissiveFactor":col[:3],"extensions":{"KHR_materials_unlit":{}}}
        if p["mat"].get("blend"): md["alphaMode"]="BLEND"; md["doubleSided"]=True
        mats.append(md); pr.append({"attributes":{"POSITION":ap},"indices":ai,"material":pi,"mode":4})
    align()
    g={"asset":{"version":"2.0"},"extensionsUsed":["KHR_materials_unlit"],"scene":0,"scenes":[{"nodes":[0]}],
       "nodes":[{"mesh":0}],"meshes":[{"primitives":pr}],"materials":mats,"bufferViews":bvs,"accessors":acc,
       "buffers":[{"byteLength":len(buf)}]}
    jb=json.dumps(g,separators=(',',':')).encode()
    while len(jb)%4: jb+=b' '
    total=12+8+len(jb)+8+len(buf); out=bytearray()
    out+=struct.pack("<III",0x46546C67,2,total)+struct.pack("<II",len(jb),0x4E4F534A)+jb+struct.pack("<II",len(buf),0x004E4942)+buf
    open(path,"wb").write(out); print("wrote",path,len(out),"prims",len(prims))

def octa(cx,cy,cz,s):
    v=[(cx+s,cy,cz),(cx-s,cy,cz),(cx,cy+s,cz),(cx,cy-s,cz),(cx,cy,cz+s),(cx,cy,cz-s)]
    f=[(0,2,4),(2,1,4),(1,3,4),(3,0,4),(2,0,5),(1,2,5),(3,1,5),(0,3,5)]
    return v,f
def add(vt,tr,v,f):
    b=len(vt); vt.extend(v)
    for (a,bb,c) in f: tr.append((b+a,b+bb,b+c))
def box_edge(a,bp,w):
    d=(bp[0]-a[0],bp[1]-a[1],bp[2]-a[2]); L=math.sqrt(sum(c*c for c in d))
    if L<1e-6: return None
    d=(d[0]/L,d[1]/L,d[2]/L)
    def cr(p,q): return (p[1]*q[2]-p[2]*q[1],p[2]*q[0]-p[0]*q[2],p[0]*q[1]-p[1]*q[0])
    def nr(p):
        l=math.sqrt(sum(c*c for c in p)) or 1.0; return (p[0]/l,p[1]/l,p[2]/l)
    rv=(0,1,0) if abs(d[1])<0.9 else (1,0,0)
    u=nr(cr(d,rv)); v=nr(cr(d,u))
    def co(p,su,sv): return (p[0]+u[0]*su*w+v[0]*sv*w,p[1]+u[1]*su*w+v[1]*sv*w,p[2]+u[2]*su*w+v[2]*sv*w)
    c=[co(a,1,1),co(a,1,-1),co(a,-1,-1),co(a,-1,1),co(bp,1,1),co(bp,1,-1),co(bp,-1,-1),co(bp,-1,1)]
    f=[(0,1,5),(0,5,4),(1,2,6),(1,6,5),(2,3,7),(2,7,6),(3,0,4),(3,4,7),(0,3,2),(0,2,1),(4,5,6),(4,6,7)]
    return c,f

# brightness tiers for nodes: (color rgba, size, weight)
NODE_TIERS=[
 {"color":[0.55,0.34,0.16,0.45],"s":0.0042,"w":26},  # dim
 {"color":[0.90,0.58,0.26,0.80],"s":0.0055,"w":44},  # mid
 {"color":[1.00,0.70,0.34,0.95],"s":0.0068,"w":22},  # bright
 {"color":[1.00,0.90,0.62,1.00],"s":0.0085,"w":8},   # hot lights
]
def pick_tier():
    r=random.uniform(0,100); a=0
    for t in NODE_TIERS:
        a+=t["w"]
        if r<=a: return t
    return NODE_TIERS[-1]

# ===== PLEXUS FIGURE =====
src=open("design/human-points.js",encoding="utf-8").read()
pts=json.loads(src[src.index("["):src.rindex("]")+1])
R=0.06;K=3;cell=R;grid={}
def key(p): return (int(p[0]//cell),int(p[1]//cell),int(p[2]//cell))
for i,p in enumerate(pts): grid.setdefault(key(p),[]).append(i)
def d2(a,b): return sum((a[k]-b[k])**2 for k in range(3))
edges=set()
for i,p in enumerate(pts):
    kx,ky,kz=key(p); cand=[]
    for ax in(-1,0,1):
        for ay in(-1,0,1):
            for az in(-1,0,1):
                for j in grid.get((kx+ax,ky+ay,kz+az),[]):
                    if j!=i:
                        dd=d2(p,pts[j])
                        if dd<=R*R: cand.append((dd,j))
    cand.sort()
    for _,j in cand[:K]: edges.add((min(i,j),max(i,j)))

node_buckets=[([],[]) for _ in NODE_TIERS]
node_tier=[]
for idx,(x,y,z) in enumerate(pts):
    ti=NODE_TIERS.index(pick_tier()); node_tier.append(ti)
    vt,tr=node_buckets[ti]; add(vt,tr,*octa(x,y,z,NODE_TIERS[ti]["s"]))
# lines brightness follows the brighter endpoint
LINE_TIERS=[{"color":[0.80,0.50,0.24,0.28],"wd":0.0005},{"color":[1.0,0.68,0.34,0.5],"wd":0.0007}]
line_buckets=[([],[]) for _ in LINE_TIERS]
for (i,j) in edges:
    bright = max(node_tier[i],node_tier[j])
    li = 1 if bright>=2 else 0
    r=box_edge(pts[i],pts[j],LINE_TIERS[li]["wd"])
    if r:
        vt,tr=line_buckets[li]; add(vt,tr,r[0],r[1])
prims=[]
for ti,t in enumerate(NODE_TIERS):
    vt,tr=node_buckets[ti]
    if vt: prims.append({"verts":vt,"tris":tr,"mat":{"color":t["color"],"blend":True}})
for li,t in enumerate(LINE_TIERS):
    vt,tr=line_buckets[li]
    if vt: prims.append({"verts":vt,"tris":tr,"mat":{"color":t["color"],"blend":True}})
build_glb("app/src/main/assets/human-plexus.glb",prims)
print("plexus edges",len(edges))

# ===== CONSTELLATIONS (with star brightness variation) =====
CONST={
 "ursa_major":{"s":[(0,1.0),(0,0.6),(0.45,0.55),(0.5,0.98),(0.95,1.05),(1.4,1.12),(1.85,1.22)],
   "e":[(0,1),(1,2),(2,3),(3,0),(3,4),(4,5),(5,6)]},
 "orion":{"s":[(0.05,1.0),(0.7,1.05),(0.30,0.55),(0.43,0.52),(0.56,0.49),(0.15,0.05),(0.66,0.08),(0.40,1.30)],
   "e":[(0,1),(0,2),(1,4),(2,3),(3,4),(2,5),(4,6),(5,6),(0,7),(1,7)]},
 "cassiopeia":{"s":[(0,0.6),(0.4,0.95),(0.8,0.5),(1.2,0.98),(1.6,0.58)],"e":[(0,1),(1,2),(2,3),(3,4)]},
 "cygnus":{"s":[(0.6,1.45),(0.6,1.0),(0.6,0.6),(0.6,0.0),(0.12,0.72),(1.08,0.72)],"e":[(0,1),(1,2),(2,3),(4,1),(1,5)]},
}
PLACE={"ursa_major":(-5.2,4.2,9.5,1.5),"orion":(4.6,0.8,9.0,1.7),"cassiopeia":(-4.0,-0.2,10.5,1.4),"cygnus":(5.4,4.4,10.5,1.5)}
STAR_TIERS=[{"color":[0.7,0.72,0.78,0.7],"s":0.04},{"color":[1.0,0.96,0.88,0.95],"s":0.06},{"color":[1.0,1.0,0.95,1.0],"s":0.085}]
star_buckets=[([],[]) for _ in STAR_TIERS]
cv=[];cf=[]
for name,c in CONST.items():
    cx,cy,cz,sc=PLACE[name]; world=[]
    for (px,py) in c["s"]:
        wx=cx+(px-0.8)*sc; wy=cy+(py-0.6)*sc; wz=cz; world.append((wx,wy,wz))
        ti=random.choice([0,1,1,2]); vt,tr=star_buckets[ti]; add(vt,tr,*octa(wx,wy,wz,STAR_TIERS[ti]["s"]))
    for (a,b) in c["e"]:
        r=box_edge(world[a],world[b],0.004)
        if r: add(cv,cf,r[0],r[1])
prims=[]
for ti,t in enumerate(STAR_TIERS):
    vt,tr=star_buckets[ti]
    if vt: prims.append({"verts":vt,"tris":tr,"mat":{"color":t["color"],"blend":True}})
prims.append({"verts":cv,"tris":cf,"mat":{"color":[0.5,0.57,0.72,0.4],"blend":True}})
build_glb("app/src/main/assets/bg-stars.glb",prims)
