(ns clojurelogo
  (:gen-class))

(use 'clojure.java.io)

(import '(java.awt Graphics Graphics2D Image AlphaComposite) )
(import '(java.awt.image BufferedImage) )
(import '(javax.imageio ImageIO))
(import '(java.util ArrayList))

(import '(javax.swing JFrame JTextArea JDialog JScrollPane JButton JLabel JPanel BoxLayout))
(import '(java.awt FlowLayout))
(import '(java.awt.event ActionListener WindowListener WindowAdapter))
(import '(java.awt.dnd DropTarget DropTargetAdapter DnDConstants))
(import '(java.awt.datatransfer DataFlavor Transferable))

(defn make-image
  "Add clojure logo on your icon"
  ([image]
    (if-let [format (re-find #"\.(\w+)$" (.getAbsolutePath image))]
      (make-image image (str (.getName image) "_clj." (nth format 1)) (nth format 1))
      (throw (Exception. "Cannot determine output file format based on filename."))))
  ([image out-filename]
    (if-let [format (re-find #"\.(\w+)$" out-filename)]
      (make-image image out-filename (nth format 1))
      (throw (Exception. "Cannot determine output file format based on filename."))))
  ([image out-filename format]
     (let [img (ImageIO/read image)
          icon-img (ImageIO/read (java.io.File. "./images/Clojure-logo-60.png"))
          imgtype (BufferedImage/TYPE_INT_RGB)
          ratio 80 ;; modify this ratio as you like
          new-width (/ (* ratio (.getWidth img)) 100)
          new-height (* new-width (/ (.getHeight icon-img) (.getWidth icon-img)))
          resized-icon-img (BufferedImage. new-width new-height (.getType icon-img))
          g2d #^Graphics2D (.createGraphics img)
          ]
      (.drawImage (.getGraphics resized-icon-img ) (.getScaledInstance icon-img new-width new-height Image/SCALE_AREA_AVERAGING)
                      0 0 new-width new-height nil)
      (.setComposite g2d (AlphaComposite/getInstance (AlphaComposite/SRC_OVER) 1.0 ))
      (.drawImage g2d resized-icon-img
        (- (.getWidth img) (.getWidth resized-icon-img))
        (- (.getHeight img) (.getHeight resized-icon-img)) nil )
       (.dispose g2d)

      (javax.imageio.ImageIO/write img format (as-file out-filename)))))


(defn dnd-listener [f]
  (proxy [DropTargetAdapter] []
	 (drop [e]
	       (apply f [e]))))

(defn drop-target [f]
  (let [dt (DropTarget.)]
       (doto dt
	     (.addDropTargetListener (dnd-listener f)))))

(defn dnd-panel [f]
  (let [panel (JPanel.)]
       (doto panel
	     (.add (JLabel. "Please Drag and Drop your icon here."))
	     (.setDropTarget (drop-target f)))))

(defn dnd-frame [f]
  (let [frame (JFrame. "Clojure Logo Adder")]
       (doto frame
	     (.add (dnd-panel f))
	     (.addWindowListener
	      (proxy [WindowAdapter] []
		     (windowClosing [e]
				    (System/exit 0))))
	     (.setSize 300 100)
	     (.setVisible true))))

(defn dnd-data-from-event [event]
  (let [transfer (.getTransferable event)]
       (.getTransferData transfer DataFlavor/javaFileListFlavor)))

(defn dnd-data-process [f]
  (fn [event]
     (.acceptDrop event DnDConstants/ACTION_REFERENCE)
     (apply f [(dnd-data-from-event event)])))

(defn drop-start []
  (dnd-frame
    (dnd-data-process
     (fn [files]  (dorun (map make-image files))))))

(drop-start)
