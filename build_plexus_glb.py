import json, re, struct

src = open("design/human-points.js","r",encoding="utf-8").read()
arr = src[src.index("["):src.rindex("]")+1]
pts = json.loads(arr)
print("points:", len(pts))

h = 0.006  # cube half-size -> 12mm dots
# unit cube corners
corners = [(-h,-h,-h),( h,-h,-h),( h, h,-h),(-h, h,-h),
           (-h,-h, h),( h,-h, h),( h, h, h),(-h, h, h)]
faces = [(0,1,2),(0,2,3),(4,6,5),(4,7,6),(0,4,5),(0,5,1),
         (1,5,6),(1,6,2),(2,6,7),(2,7,3),(3,7,4),(3,4,0)]

positions = bytearray()
indices = bytearray()
minp = [ 1e9, 1e9, 1e9]; maxp = [-1e9,-1e9,-1e9]
vbase = 0
for (px,py,pz) in pts:
    for (cx,cy,cz) in corners:
        x,y,z = px+cx, py+cy, pz+cz
        positions += struct.pack("<fff", x,y,z)
        minp[0]=min(minp[0],x); minp[1]=min(minp[1],y); minp[2]=min(minp[2],z)
        maxp[0]=max(maxp[0],x); maxp[1]=max(maxp[1],y); maxp[2]=max(maxp[2],z)
    for (a,b,c) in faces:
        indices += struct.pack("<III", vbase+a, vbase+b, vbase+c)
    vbase += 8

numVerts = len(pts)*8
numIdx = len(pts)*36

# pad bin chunks to 4 bytes
def pad4(b, fill=b'\x00'):
    while len(b)%4: b += fill
    return b

idx_off = 0
idx_len = len(indices)
pos_off = pad4(bytearray(idx_len)) and idx_len + ((4-idx_len%4)%4)
# build buffer: indices first (aligned), then positions
buf = bytearray()
buf += indices
while len(buf)%4: buf += b'\x00'
pos_byteoffset = len(buf)
buf += positions
while len(buf)%4: buf += b'\x00'

gltf = {
  "asset":{"version":"2.0","generator":"QuestSync plexus builder"},
  "extensionsUsed":["KHR_materials_unlit"],
  "scenes":[{"nodes":[0]}],
  "scene":0,
  "nodes":[{"mesh":0,"name":"plexus"}],
  "meshes":[{"name":"plexus","primitives":[{
      "attributes":{"POSITION":1},"indices":0,"material":0,"mode":4}]}],
  "materials":[{
      "name":"ember",
      "pbrMetallicRoughness":{"baseColorFactor":[0.945,0.604,0.243,1.0],
                              "metallicFactor":0.0,"roughnessFactor":1.0},
      "emissiveFactor":[0.945,0.604,0.243],
      "extensions":{"KHR_materials_unlit":{}}}],
  "bufferViews":[
     {"buffer":0,"byteOffset":0,"byteLength":idx_len,"target":34963},
     {"buffer":0,"byteOffset":pos_byteoffset,"byteLength":len(positions),"target":34964}],
  "accessors":[
     {"bufferView":0,"componentType":5125,"count":numIdx,"type":"SCALAR"},
     {"bufferView":1,"componentType":5126,"count":numVerts,"type":"VEC3",
      "min":minp,"max":maxp}],
  "buffers":[{"byteLength":len(buf)}]
}

json_bytes = json.dumps(gltf,separators=(',',':')).encode("utf-8")
while len(json_bytes)%4: json_bytes += b' '
while len(buf)%4: buf += b'\x00'

total = 12 + 8 + len(json_bytes) + 8 + len(buf)
out = bytearray()
out += struct.pack("<III", 0x46546C67, 2, total)
out += struct.pack("<II", len(json_bytes), 0x4E4F534A); out += json_bytes
out += struct.pack("<II", len(buf), 0x004E4942); out += buf

open("app/src/main/assets/human-plexus.glb","wb").write(out)
print("wrote app/src/main/assets/human-plexus.glb", len(out), "bytes; verts", numVerts, "idx", numIdx)
