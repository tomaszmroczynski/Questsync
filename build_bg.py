import json, struct, math, random

def build_glb(path, prims):
    buf=bytearray(); bufferViews=[]; accessors=[]; materials=[]; primitives=[]
    def align():
        while len(buf)%4: buf.append(0)
    for pi,pr in enumerate(prims):
        verts=pr["verts"]; tris=pr["tris"]
        idx=bytearray()
        for (a,b,c) in tris: idx+=struct.pack("<III",a,b,c)
        align(); io=len(buf); buf+=idx
        align(); po=len(buf)
        mn=[1e9]*3; mx=[-1e9]*3; pos=bytearray()
        for (x,y,z) in verts:
            pos+=struct.pack("<fff",x,y,z)
            mn[0]=min(mn[0],x);mn[1]=min(mn[1],y);mn[2]=min(mn[2],z)
            mx[0]=max(mx[0],x);mx[1]=max(mx[1],y);mx[2]=max(mx[2],z)
        buf+=pos
        bvi=len(bufferViews); bufferViews.append({"buffer":0,"byteOffset":io,"byteLength":len(idx),"target":34963})
        bvp=len(bufferViews); bufferViews.append({"buffer":0,"byteOffset":po,"byteLength":len(pos),"target":34964})
        ai=len(accessors); accessors.append({"bufferView":bvi,"componentType":5125,"count":len(tris)*3,"type":"SCALAR"})
        ap=len(accessors); accessors.append({"bufferView":bvp,"componentType":5126,"count":len(verts),"type":"VEC3","min":mn,"max":mx})
        e=pr["mat"]["emissive"]
        materials.append({"pbrMetallicRoughness":{"baseColorFactor":[e[0],e[1],e[2],1.0],"metallicFactor":0.0,"roughnessFactor":1.0},
                          "emissiveFactor":e,"extensions":{"KHR_materials_unlit":{}}})
        primitives.append({"attributes":{"POSITION":ap},"indices":ai,"material":pi,"mode":4})
    align()
    gltf={"asset":{"version":"2.0"},"extensionsUsed":["KHR_materials_unlit"],"scene":0,"scenes":[{"nodes":[0]}],
          "nodes":[{"mesh":0}],"meshes":[{"primitives":primitives}],"materials":materials,
          "bufferViews":bufferViews,"accessors":accessors,"buffers":[{"byteLength":len(buf)}]}
    jb=json.dumps(gltf,separators=(',',':')).encode()
    while len(jb)%4: jb+=b' '
    total=12+8+len(jb)+8+len(buf); out=bytearray()
    out+=struct.pack("<III",0x46546C67,2,total)
    out+=struct.pack("<II",len(jb),0x4E4F534A)+jb
    out+=struct.pack("<II",len(buf),0x004E4942)+buf
    open(path,"wb").write(out); print("wrote",path,len(out))

def octa(cx,cy,cz,s):
    v=[(cx+s,cy,cz),(cx-s,cy,cz),(cx,cy+s,cz),(cx,cy-s,cz),(cx,cy,cz+s),(cx,cy,cz-s)]
    f=[(0,2,4),(2,1,4),(1,3,4),(3,0,4),(2,0,5),(1,2,5),(3,1,5),(0,3,5)]
    return v,f
def add(vt,tr,v,f):
    b=len(vt); vt.extend(v)
    for (a,bb,c) in f: tr.append((b+a,b+bb,b+c))
def box_edge(a,bp,w):
    ax,ay,az=a; bx,by,bz=bp
    dx,dy,dz=bx-ax,by-ay,bz-az; L=math.sqrt(dx*dx+dy*dy+dz*dz)
    if L<1e-6: return None
    d=(dx/L,dy/L,dz/L)
    def cross(p,q): return (p[1]*q[2]-p[2]*q[1],p[2]*q[0]-p[0]*q[2],p[0]*q[1]-p[1]*q[0])
    def nrm(p):
        l=math.sqrt(sum(c*c for c in p)) or 1.0; return (p[0]/l,p[1]/l,p[2]/l)
    refv=(0,1,0) if abs(d[1])<0.9 else (1,0,0)
    u=nrm(cross(d,refv)); v=nrm(cross(d,u))
    def cor(p,su,sv): return (p[0]+u[0]*su*w+v[0]*sv*w,p[1]+u[1]*su*w+v[1]*sv*w,p[2]+u[2]*su*w+v[2]*sv*w)
    c=[cor(a,1,1),cor(a,1,-1),cor(a,-1,-1),cor(a,-1,1),cor(bp,1,1),cor(bp,1,-1),cor(bp,-1,-1),cor(bp,-1,1)]
    f=[(0,1,5),(0,5,4),(1,2,6),(1,6,5),(2,3,7),(2,7,6),(3,0,4),(3,4,7),(0,3,2),(0,2,1),(4,5,6),(4,6,7)]
    return c,f

# ---- portal ring (XY plane, vertical, faces viewer) ----
ri,ro,seg=1.32,1.40,96
rv=[];rf=[]
for k in range(seg):
    a0=2*math.pi*k/seg; a1=2*math.pi*(k+1)/seg
    p0i=(ri*math.cos(a0),ri*math.sin(a0),0); p0o=(ro*math.cos(a0),ro*math.sin(a0),0)
    p1i=(ri*math.cos(a1),ri*math.sin(a1),0); p1o=(ro*math.cos(a1),ro*math.sin(a1),0)
    add(rv,rf,[p0i,p0o,p1o,p1i],[(0,1,2),(0,2,3),(0,2,1),(0,3,2)])
build_glb("app/src/main/assets/portal-ring.glb",[{"verts":rv,"tris":rf,"mat":{"emissive":[1.0,0.84,0.55]}}])

# ---- background constellation (distant plexus field) ----
random.seed(7)
N=520
pts=[]
for _ in range(N):
    x=random.uniform(-9,9); y=random.uniform(-2.5,6.5); z=random.uniform(7.5,10.5)
    pts.append((x,y,z))
nv=[];nf=[]
for (x,y,z) in pts:
    add(nv,nf,*octa(x,y,z,0.05))
# sparse nearest-neighbor lines
cell=1.6; grid={}
def key(p): return (int(p[0]//cell),int(p[1]//cell),int(p[2]//cell))
for i,p in enumerate(pts): grid.setdefault(key(p),[]).append(i)
def d2(a,b): return sum((a[k]-b[k])**2 for k in range(3))
edges=set()
for i,p in enumerate(pts):
    kx,ky,kz=key(p); best=[]
    for ax in(-1,0,1):
        for ay in(-1,0,1):
            for az in(-1,0,1):
                for j in grid.get((kx+ax,ky+ay,kz+az),[]):
                    if j!=i:
                        dd=d2(p,pts[j])
                        if dd<=(1.5*1.5): best.append((dd,j))
    best.sort()
    for _,j in best[:1]:
        edges.add((min(i,j),max(i,j)))
lv=[];lf=[]
for (i,j) in edges:
    r=box_edge(pts[i],pts[j],0.006)
    if r: add(lv,lf,r[0],r[1])
build_glb("app/src/main/assets/bg-plexus.glb",[
    {"verts":nv,"tris":nf,"mat":{"emissive":[0.32,0.20,0.09]}},
    {"verts":lv,"tris":lf,"mat":{"emissive":[0.18,0.11,0.05]}},
])
print("bg points",N,"edges",len(edges))
