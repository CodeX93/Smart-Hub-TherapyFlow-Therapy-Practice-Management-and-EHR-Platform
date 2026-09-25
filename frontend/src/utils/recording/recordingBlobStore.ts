const DB_NAME = "therapy-flow-recording";
const DB_VERSION = 1;
const STORE = "failed-chunks";

export interface StoredFailedChunk {
  key: string;
  uploadId: string;
  sessionId?: number;
  index: number;
  durationSec: number;
  mime: string;
  blob: Blob;
  savedAt: number;
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === "undefined") {
      reject(new Error("IndexedDB not available"));
      return;
    }
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE)) {
        const store = db.createObjectStore(STORE, { keyPath: "key" });
        store.createIndex("byUploadId", "uploadId", { unique: false });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

function tx(db: IDBDatabase, mode: IDBTransactionMode) {
  return db.transaction(STORE, mode).objectStore(STORE);
}

function makeKey(uploadId: string, index: number): string {
  return `${uploadId}::${index}`;
}

export async function putFailedChunk(
  entry: Omit<StoredFailedChunk, "key" | "savedAt">,
): Promise<void> {
  try {
    const db = await openDb();
    await new Promise<void>((resolve, reject) => {
      const req = tx(db, "readwrite").put({
        ...entry,
        key: makeKey(entry.uploadId, entry.index),
        savedAt: Date.now(),
      });
      req.onsuccess = () => resolve();
      req.onerror = () => reject(req.error);
    });
    db.close();
  } catch (error) {
    console.warn("[recording-blob-store] put failed:", error);
  }
}

export async function deleteFailedChunk(uploadId: string, index: number): Promise<void> {
  try {
    const db = await openDb();
    await new Promise<void>((resolve, reject) => {
      const req = tx(db, "readwrite").delete(makeKey(uploadId, index));
      req.onsuccess = () => resolve();
      req.onerror = () => reject(req.error);
    });
    db.close();
  } catch (error) {
    console.warn("[recording-blob-store] delete failed:", error);
  }
}

export async function clearFailedChunksForUpload(uploadId: string): Promise<void> {
  try {
    const db = await openDb();
    await new Promise<void>((resolve, reject) => {
      const store = tx(db, "readwrite");
      const idx = store.index("byUploadId");
      const cursorReq = idx.openCursor(IDBKeyRange.only(uploadId));
      cursorReq.onsuccess = () => {
        const cursor = cursorReq.result;
        if (cursor) {
          cursor.delete();
          cursor.continue();
        } else {
          resolve();
        }
      };
      cursorReq.onerror = () => reject(cursorReq.error);
    });
    db.close();
  } catch (error) {
    console.warn("[recording-blob-store] clear failed:", error);
  }
}
